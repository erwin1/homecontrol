package evcharging.impl.sma;

import evcharging.services.EVCharger;
import evcharging.services.MeterReading;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

/**
 * SMA implementation for the EVCharger interface.
 * It uses a reverse engineered version of the device local SMA web admin UI .
 */
@ApplicationScoped
public class SMACharger implements EVCharger {
    @ConfigProperty(name = "EVCHARGING_CHARGER_IP")
    String chargerIp;
    @ConfigProperty(name = "EVCHARGING_CHARGER_USERNAME")
    String chargerUserName;
    @ConfigProperty(name = "EVCHARGING_CHARGER_PASSWORD")
    String chargerPassword;

    private String token;

    @Override
    public State getState() {
        try {
            return getStatusInternal();
        } catch (SMAAuthException e) {
            token = null;
            try {
                return getStatusInternal();
            } catch (SMAAuthException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
    }

    public State getStatusInternal() throws SMAAuthException {
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/widgets/emobility?componentId=Plant:1");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+token);

            if (conn.getResponseCode() == 401) {
                token = null;
                throw new SMAAuthException();
            }
            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new SMAAuthException();
            }

            State status = State.valueOf(object.getString("chargeStatus"));
            return status;
        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Retry(maxRetries = 10, delay = 3, delayUnit = ChronoUnit.SECONDS)
    public List<MeterReading> getHistoricalReadings(ZonedDateTime startTime, ZonedDateTime endTime, String res) throws SMAAuthException {
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/measurements/search/");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+token);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            String resolution = "";
            if (res != null) {//OneDay
                resolution = "\"resolution\":\""+res+"\",";
            }
            String aggregate = "";
//            String aggregate = "Avg";
//            String aggregate = "Dif";
              writer.write("{\"queryItems\":[{\"componentId\":\"Plant:1\",\"channelId\":\"Measurement.Metering.GridMs.TotWhIn.ChaSta\",\"timezone\":\"Europe/Brussels\","+resolution+"\"aggregate\":\""+aggregate+"\",\"multiAggregate\":\"Sum\"}],\"dateTimeBegin\":\""+startTime.withZoneSameInstant(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)+"\",\"dateTimeEnd\":\""+endTime.withZoneSameInstant(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)+"\"}");
            writer.flush();

            if (conn.getResponseCode() == 401) {
                token = null;
                throw new SMAAuthException();
            }
            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
            JsonArray array = new JsonArray(body);
            JsonArray values = array.getJsonObject(0).getJsonArray("values");
            List<MeterReading> list = new LinkedList<>();
            for(int i = 0; i < values.size(); i++) {
                if (values.getJsonObject(i).containsKey("value")) {
                    list.add(new MeterReading(ZonedDateTime.parse(values.getJsonObject(i).getString("time")),
                            values.getJsonObject(i).getInteger("value") / 4 * 4));
                }
            }
            return list;
        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Retry(maxRetries = 10, delay = 3, delayUnit = ChronoUnit.SECONDS)
    public long getConsumptionMeterReading() throws SMAAuthException {
        return getReadingByKey("Measurement.Metering.GridMs.TotWhIn.ChaSta");
    }

    @Retry(maxRetries = 2, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public long getLivePowerMeterReading() throws SMAAuthException {
        return getReadingByKey("Measurement.Metering.GridMs.TotWIn.ChaSta");
    }

    private long getReadingByKey(String key) throws SMAAuthException {
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/measurements/live/");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+token);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("[{\"componentId\":\"Plant:1\"}]");
            writer.flush();

            if (conn.getResponseCode() == 401) {
                token = null;
                throw new SMAAuthException();
            }
            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
            JsonArray array = new JsonArray(body);
            for(int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                if (obj.getString("channelId").equals(key)) {
                    return obj.getJsonArray("values").getJsonObject(0).getLong("value");
                }
            }
        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private String authenticate() {
        try {
            URLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/token");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("grant_type=password&username="+chargerUserName+"&password="+chargerPassword);
            writer.flush();

            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();

            JsonObject obj = new JsonObject(body);
            return obj.getString("access_token");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
