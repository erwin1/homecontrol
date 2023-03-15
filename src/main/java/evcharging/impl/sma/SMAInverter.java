package evcharging.impl.sma;

import evcharging.services.ElectricityMeter;
import evcharging.services.PowerValues;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.net.URLConnection;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

/**
 * SMA implementation of the ElectricityMeter interface.
 * It uses a reverse engineered version of the SMA inverter's local web admin UI .
 */
@ApplicationScoped
public class SMAInverter implements ElectricityMeter {
    public static final Logger LOGGER = Logger.getLogger(SMAInverter.class.getName());
    @ConfigProperty(name = "EVCHARING_INVERTER_IP")
    String inverterIp;
    @ConfigProperty(name = "EVCHARING_INVERTER_PASSWORD")
    String inverterPassword;

    private String sid;

    @PreDestroy
    public void preDestroy() {
        LOGGER.info("logging out SMA inverter");
        logout();
    }

    @Override
    public PowerValues getCurrentValues() {
        try {
            return readInternal();
        } catch (SMAAuthException e) {
            sid = null;
            try {
                return readInternal();
            } catch (SMAAuthException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
    }

    @Override
    public long getConsumptionMeterReadingAt(ZonedDateTime timestamp) {
        if (ZonedDateTime.now().toEpochSecond() - timestamp.toEpochSecond() <= 5) {
            LOGGER.fine("Waiting 5s to get a better reading");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return getAbsoluteFromGridMeterReadingAtTimestampInternal(timestamp);
        } catch (SMAAuthException e) {
            sid = null;
            try {
                return getAbsoluteFromGridMeterReadingAtTimestampInternal(timestamp);
            } catch (SMAAuthException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
    }

    @Override
    public long getCurrentConsumptionMeterReading() {
        try {
            return getAbsoluteFromGridMeterReadingInternal();
        } catch (SMAAuthException e) {
            sid = null;
            try {
                return getAbsoluteFromGridMeterReadingInternal();
            } catch (SMAAuthException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
    }

    long getAbsoluteFromGridMeterReadingAtTimestampInternal(ZonedDateTime timestamp) throws SMAAuthException {
        try {
            if (sid == null) {
                sid = authenticate();
            }
            URLConnection conn = HttpHelper.openConnection("https://"+ inverterIp +"/dyn/getLogger.json?sid="+sid);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            long start = timestamp.toEpochSecond();
            long end = start;

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("{\"destDev\":[],\"key\":28736,\"tStart\":"+start+",\"tEnd\":"+end+"}");
            writer.flush();

            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();

//            System.out.println("BODY = "+body);

            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new evcharging.impl.sma.SMAAuthException();
            }
            return object.getJsonObject("result").getJsonArray("0199-xxxxx04E").getJsonObject(0).getInteger("v");

        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    long getAbsoluteFromGridMeterReadingInternal() throws SMAAuthException {
        try {
            if (sid == null) {
                sid = authenticate();
            }
            URLConnection conn = HttpHelper.openConnection("https://"+ inverterIp +"/dyn/getAllOnlValues.json?sid="+sid);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("{\"destDev\":[]}");
            writer.flush();

            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();

//            System.out.println("BODY = "+body);

            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new evcharging.impl.sma.SMAAuthException();
            }

            long reading = object.getJsonObject("result")
                    .getJsonObject("0199-xxxxx04E")
                    .getJsonObject("6400_00469200")
                    .getJsonArray("1")
                    .getJsonObject(0)
                    .getLong("val", 0L);
            return reading;

        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PowerValues readInternal() throws SMAAuthException {
        try {
            if (sid == null) {
                sid = authenticate();
            }
            URLConnection conn = HttpHelper.openConnection("https://"+ inverterIp +"/dyn/getAllOnlValues.json?sid="+sid);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("{\"destDev\":[]}");
            writer.flush();

            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();

//            System.out.println("BODY = "+body);

            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new evcharging.impl.sma.SMAAuthException();
            }

            int fromGrid = object.getJsonObject("result")
                    .getJsonObject("0199-xxxxx04E")
                    .getJsonObject("6100_40463700")
                    .getJsonArray("1")
                    .getJsonObject(0)
                    .getInteger("val", 0);
            int toGrid = object.getJsonObject("result")
                    .getJsonObject("0199-xxxxx04E")
                    .getJsonObject("6100_40463600")
                    .getJsonArray("1")
                    .getJsonObject(0)
                    .getInteger("val", 0);
            String x = object.getJsonObject("result")
                    .getJsonObject("0199-xxxxx04E")
                    .getJsonObject("6100_40263F00")
                    .getJsonArray("1")
                    .getJsonObject(0)
                    .getString("val");
            int fromPV = 0;
            if (x != null) {
                fromPV = Integer.parseInt(x);
            }

            return new PowerValues(toGrid, fromGrid, fromPV, fromGrid+fromPV-toGrid);

        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String authenticate() {
        try {
            URLConnection conn = HttpHelper.openConnection("https://"+ inverterIp +"/dyn/login.json");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("{\"right\":\"usr\",\"pass\":\""+ inverterPassword +"\"}");
            writer.close();
            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
//            System.out.println("BODY = "+body);

            JsonObject object = new JsonObject(body);
            if (object.getInteger("err") != null) {
                throw new RuntimeException("auth err "+object.getInteger("err"));
            }
            String sid = object.getJsonObject("result").getString("sid");
            LOGGER.info("successfully retrieved SMA sid");
            return sid;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void logout() {
        if (sid != null) {
            try {
                URLConnection conn = HttpHelper.openConnection("https://"+ inverterIp +"/dyn/logout.json?sid=" + sid);
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write("{}");
                writer.flush();

                BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
                LOGGER.info("logout response = "+new String(reader.readAllBytes()));

                sid = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
