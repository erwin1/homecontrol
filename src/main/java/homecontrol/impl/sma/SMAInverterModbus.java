package homecontrol.impl.sma;

import homecontrol.services.solar.Inverter;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Reads live and cumulative production values from an SMA Sunny Boy 5 inverter
 * over Modbus TCP, using only the Java standard library (no external dependencies).
 *
 * <p>Expanded from the total-yield-only version to add "live yield" (the AC power
 * the inverter is producing right now) and today's daily yield.
 *
 * <p>Prerequisite: Modbus TCP must be ENABLED on the inverter (it is off by
 * default). Enable it in the web UI under
 *   Device Configuration -> External Communication -> Modbus -> TCP Server (port 502).
 *
 * <p>SMA Modbus register reference (holding registers, function code 0x03):
 *   30513  Total yield   U64  4 registers  Wh   FIX0   (lifetime energy)
 *   30535  Daily yield   U32  2 registers  Wh   FIX0   (energy produced today)
 *   30775  AC power      S32  2 registers  W    FIX0   (live output; SIGNED)
 *   30773  DC power [1]  S32  2 registers  W    FIX0   (easy add if wanted)
 *   30777  AC power L1   S32  2 registers  W    FIX0   (per-phase, easy add)
 */
@ApplicationScoped
public class SMAInverterModbus implements Inverter {
    public static final Logger LOGGER = Logger.getLogger(SMAInverterModbus.class.getName());
    @ConfigProperty(name = "EVCHARGING_INVERTER_IP")
    String inverterIp;

    // ---- Configuration ----------------------------------------------------
    private final int port = 502;
    private final int unitId = 3;
    private final int timeoutMs = 5000;

    private static final int REG_TOTAL_YIELD = 30513; // U64, 4 registers, Wh
    private static final int REG_DAILY_YIELD = 30535; // U32, 2 registers, Wh
    private static final int REG_AC_POWER    = 30775; // S32, 2 registers, W

    // SMA "not a number" sentinels.
    private static final long U64_NAN = 0xFFFFFFFFFFFFFFFFL;
    private static final long U32_NAN = 0xFFFFFFFFL;
    public  static final int  S32_NAN = Integer.MIN_VALUE; // 0x80000000


    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    @Asynchronous
    public Uni<Integer> getCurrentYield() {
        return Uni.createFrom().item(getLivePower());
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public int getYieldMeterReading() {
        return (int) getTotalYield();
    }


    private int getLivePower() {
        int powerW = 0;
        try {
            powerW = readCurrentPowerWatts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (powerW == S32_NAN) {
            LOGGER.fine("Live power:   not producing (inverter in standby)");
            powerW = 0;
        } else {
            LOGGER.fine("Live power: "+powerW+" W");
        }
        return powerW;
    }

    private long getTotalYield() {
        try {
            return readTotalYieldWh();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Live yield: the AC power the inverter is currently producing, in watts
     * (register 30775, S32). Returns {@link #S32_NAN} when the inverter reports
     * the value as unavailable (typically standby at night).
     */
    public int readCurrentPowerWatts() throws IOException {
        return readSigned32(REG_AC_POWER);
    }

    /** Daily yield: energy produced today, in watt-hours (register 30535, U32). */
    public long readDailyYieldWh() throws IOException {
        long v = readUnsigned32(REG_DAILY_YIELD);
        return (v == U32_NAN) ? 0 : v;
    }

    /** Total yield: lifetime energy, in watt-hours (register 30513, U64). */
    public long readTotalYieldWh() throws IOException {
        int[] regs = readHoldingRegisters(REG_TOTAL_YIELD, 4);
        long value = 0;
        for (int r : regs) {
            value = (value << 16) | (r & 0xFFFF);
        }
        return (value == U64_NAN) ? 0 : value;
    }

    // ---- Typed register reads --------------------------------------------

    /** Reads a 2-register big-endian SIGNED 32-bit value. */
    private int readSigned32(int address) throws IOException {
        int[] r = readHoldingRegisters(address, 2);
        // Building the result directly as an int yields the correct signed value:
        // the high register's top bit becomes the int's sign bit.
        return ((r[0] & 0xFFFF) << 16) | (r[1] & 0xFFFF);
    }

    /** Reads a 2-register big-endian UNSIGNED 32-bit value. */
    private long readUnsigned32(int address) throws IOException {
        int[] r = readHoldingRegisters(address, 2);
        return ((long) (r[0] & 0xFFFF) << 16) | (r[1] & 0xFFFF);
    }

    // ---- Modbus plumbing --------------------------------------------------

    /**
     * Issues a Modbus TCP "Read Holding Registers" (FC 0x03) request and returns
     * the raw 16-bit register values.
     */
    public int[] readHoldingRegisters(int startAddress, int quantity) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(inverterIp, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            final int transactionId = 1;

            // ---- Build request: MBAP header (7 bytes) + PDU (5 bytes) ----
            byte[] request = new byte[12];
            request[0] = (byte) (transactionId >> 8);   // Transaction Id (hi)
            request[1] = (byte) (transactionId & 0xFF); // Transaction Id (lo)
            request[2] = 0x00;                          // Protocol Id (hi) = 0 (Modbus)
            request[3] = 0x00;                          // Protocol Id (lo)
            request[4] = 0x00;                          // Length (hi)
            request[5] = 0x06;                          // Length (lo) = 6 bytes follow
            request[6] = (byte) unitId;                 // Unit Id
            request[7] = 0x03;                          // Function: Read Holding Registers
            request[8] = (byte) (startAddress >> 8);    // Start address (hi)
            request[9] = (byte) (startAddress & 0xFF);  // Start address (lo)
            request[10] = (byte) (quantity >> 8);       // Quantity (hi)
            request[11] = (byte) (quantity & 0xFF);     // Quantity (lo)

            out.write(request);
            out.flush();

            // ---- Read response ----
            byte[] head = readFully(in, 6); // Transaction(2)+Protocol(2)+Length(2)
            int length = ((head[4] & 0xFF) << 8) | (head[5] & 0xFF);
            if (length < 3) {
                throw new IOException("Malformed response: length field = " + length);
            }
            byte[] body = readFully(in, length); // UnitId(1)+Function(1)+ByteCount(1)+Data

            int functionCode = body[1] & 0xFF;
            if ((functionCode & 0x80) != 0) {
                int exceptionCode = body[2] & 0xFF;
                throw new IOException("Modbus exception for function " + (functionCode & 0x7F)
                        + ", exception code " + exceptionCode);
            }
            if (functionCode != 0x03) {
                throw new IOException("Unexpected function code in response: " + functionCode);
            }

            int byteCount = body[2] & 0xFF;
            if (byteCount != quantity * 2) {
                throw new IOException("Unexpected byte count: " + byteCount
                        + " (expected " + (quantity * 2) + ")");
            }

            int[] registers = new int[quantity];
            for (int i = 0; i < quantity; i++) {
                int hi = body[3 + i * 2] & 0xFF;
                int lo = body[3 + i * 2 + 1] & 0xFF;
                registers[i] = (hi << 8) | lo;
            }
            return registers;
        }
    }

    /** Reads exactly {@code n} bytes from the stream or throws. */
    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int read = in.read(buf, off, n - off);
            if (read < 0) {
                throw new IOException("Unexpected end of stream (read " + off + " of " + n + " bytes)");
            }
            off += read;
        }
        return buf;
    }

}