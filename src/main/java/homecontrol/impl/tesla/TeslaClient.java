package homecontrol.impl.tesla;

import homecontrol.services.ev.EVState;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Code that can:
 *  - request state
 *  - send commands
 *  from a Tesla vehicle via BLE using the tesla-control command
 */
public class TeslaClient {
    public static final Logger LOGGER = Logger.getLogger(TeslaClient.class.getName());

    private static Object commandLock = new Object();

    private String vin;
    private String keyName;
    private String tokenName;
    private String sdkDir;
    private String cacheFile;

    public TeslaClient(String vin, String keyName, String tokenName, String sdkDir, String cacheFile) {
        this.vin = vin;
        this.keyName = keyName;
        this.tokenName = tokenName;
        this.sdkDir = sdkDir;
        this.cacheFile = cacheFile;
    }

    public EVState getChargeState() throws TeslaException {
        String rspString = executeCommand("state", "charge");
        JsonObject rsp = new JsonObject(rspString);
        JsonObject chargeState = rsp.getJsonObject("chargeState");
        EVState state = new EVState();
        state.setTimestamp(Instant.now());
        state.setCharge_amps(chargeState.getInteger("chargingAmps"));
        state.setCharge_current_request(chargeState.getInteger("chargeCurrentRequest"));
        state.setCharge_current_request_max(chargeState.getInteger("chargeCurrentRequestMax"));
        state.setCharger_actual_current(chargeState.getInteger("chargerActualCurrent"));
        //charging_state: Disconnected Charging Stopped Starting
        JsonObject chargingState = chargeState.getJsonObject("chargingState");
        state.setCharging_state(chargingState.fieldNames().iterator().next());
        LOGGER.info("chargingState = "+chargingState+" "+state.getCharging_state());
        state.setCharge_limit_soc(chargeState.getInteger("chargeLimitSoc"));
        state.setBattery_level(chargeState.getInteger("usableBatteryLevel")); // batteryLevel / usableBatteryLevel ?
        return state;
    }

    public boolean openChargePortDoor() throws TeslaException {
        executeCommand("wake", null);
        executeCommand("charge-port-open", null);
        executeCommand("unlock", null);
        return true;
    }

    public boolean setChargingAmps(int amps) throws TeslaException {
        executeCommand("charging-set-amps", String.valueOf(amps));
        return true;
    }

    public boolean setScheduledCharging(boolean enabled, int time) throws TeslaException {
        if (enabled) {
            executeCommand("charging-schedule ", String.valueOf(time));
        } else {
            executeCommand("charging-schedule-cancel", null);
        }
        return true;
    }

    public void wakeup() throws TeslaException {
        executeCommand("wake", null);
    }

    public boolean stopCharging() throws TeslaException {
        executeCommand("charging-stop", null);
        return true;
    }

    public boolean startCharging() throws TeslaException {
        executeCommand("charging-start", null);
        return true;
    }

    public boolean isVehicleOnline() throws TeslaException {
        String rsp = executeCommand("body-controller-state", null);
        JsonObject responseObject = new JsonObject(rsp);
        //VEHICLE_SLEEP_STATUS_ASLEEP / VEHICLE_SLEEP_STATUS_AWAKE
        String vehicleSleepStatus = responseObject.getString("vehicleSleepStatus", "");
        return "VEHICLE_SLEEP_STATUS_AWAKE".equals(vehicleSleepStatus);
    }

    private String executeCommand(String command, String opt) throws TeslaException {
        synchronized (commandLock) {
            try {
                LOGGER.info("executing command: " + command + " " + (opt != null ? opt + " " : ""));
                ProcessBuilder builder = new ProcessBuilder("./tesla-control", "-ble", "-debug", command);
                if (opt != null) {
                    builder.command().add(opt);
                }
                builder.directory(new File(sdkDir));
                builder.environment().put("TESLA_KEY_NAME", keyName);
                builder.environment().put("TESLA_TOKEN_NAME", tokenName);
                builder.environment().put("TESLA_CACHE_FILE", cacheFile);
                builder.environment().put("TESLA_VIN", vin);
                Process p = builder.start();
                p.waitFor(2, TimeUnit.MINUTES);
                if (p.exitValue() != 0) {
                    String error = p.errorReader().lines().collect(Collectors.joining("\n"));
                    LOGGER.warning("Error sending BLE command " + error);
                    throw new TeslaException(408, "command returned " + p.exitValue());
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder rsp = new StringBuilder();
                String l;
                while((l=r.readLine())!=null) {
                    rsp.append(l).append("\n");
                }
                r.close();
                LOGGER.info("command " + command + " " + (opt != null ? opt + " " : "") + " executed successfully");
                LOGGER.fine("response = "+rsp);
                return rsp.toString();
            } catch (TeslaException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "unexpected exception running command", e);
                throw new TeslaException(0, "unexpected exception running command " + e);
            }
        }
    }

}
