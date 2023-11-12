package homecontrol.impl.sma;

import homecontrol.services.solar.Inverter;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SMAInverter implements Inverter {
    public static final Logger LOGGER = Logger.getLogger(SMAInverter.class.getName());
    @ConfigProperty(name = "EVCHARGING_INVERTER_IP")
    String inverterIp;
    @ConfigProperty(name = "EVCHARGING_INVERTER_PASSWORD")
    String inverterPassword;

    String sid;

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    @Asynchronous
    public Uni<Integer> getCurrentYield() {
        return Uni.createFrom().item(getLiveSMAPowerDataInternal());
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public int getYieldMeterReading() {
        return (int) getAbsolutePVMeterReadingInternal();
    }

    @PreDestroy
    public void preDestroy() {
        LOGGER.info("logging out SMA inverter");
        logout();
    }

    long getAbsolutePVMeterReadingInternal() {
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

            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                sid = null;
                throw new RuntimeException("auth err "+object.getString("err"));
            }

            long reading = object.getJsonObject("result")
                    .getJsonObject("0199-xxxxx04E")
                    .getJsonObject("6400_00260100")
                    .getJsonArray("1")
                    .getJsonObject(0)
                    .getLong("val", 0L);
            return reading;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getLiveSMAPowerDataInternal() {
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

            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                sid = null;
                throw new RuntimeException("auth err "+object.getString("err"));
            }

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

            return fromPV;
        } catch (RuntimeException e) {
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

            JsonObject object = new JsonObject(body);
            if (object.getInteger("err") != null) {
                sid = null;
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
