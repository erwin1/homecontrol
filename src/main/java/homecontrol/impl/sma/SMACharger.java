package homecontrol.impl.sma;

import homecontrol.services.ev.Charger;
import homecontrol.services.ev.StateRefresh;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class SMACharger implements Charger {

    @ConfigProperty(name = "EVCHARGING_CHARGER_IP")
    String chargerIp;
    @ConfigProperty(name = "EVCHARGING_CHARGER_USERNAME")
    String chargerUserName;
    @ConfigProperty(name = "EVCHARGING_CHARGER_PASSWORD")
    String chargerPassword;

    private String token;

    private State state;

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public State getCurrentState(StateRefresh stateRefresh) {
        var state = switch (stateRefresh) {
            case CACHED_OR_NULL -> this.state;
            case CACHED -> {
                if (this.state == null) {
                    yield getStateInternal();
                }
                yield this.state;
            }
            case REFRESH_IF_ONLINE, REFRESH_ALWAYS -> getStateInternal();
        };
        if (state != null) {
            this.state = state;
        }
        return this.state;
    }

    @Override
    @Asynchronous
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public Uni<Integer> getActivePower() {
        int activePower = (int) getLivePowerMeterReading();
        return Uni.createFrom().item(activePower);
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public int getChargingMeterReading() {
        return (int) getConsumptionMeterReading();
    }

    public State getStateInternal() {
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/widgets/emobility?componentId=Plant:1");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+token);

            if (conn.getResponseCode() == 401) {
                token = null;
                throw new RuntimeException("auth error");
            }
            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new RuntimeException("error from charger "+object.getString("err"));
            }

            State status = State.valueOf(object.getString("chargeStatus"));
            return status;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    public List<MeterReading> getHistoricalReadings(ZonedDateTime startTime, ZonedDateTime endTime, String res) {
//        try {
//            if (token == null) {
//                token = authenticate();
//            }
//            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/measurements/search/");
//            conn.setDoOutput(true);
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Accept", "application/json");
//            conn.setRequestProperty("Authorization", "Bearer "+token);
//
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
//            String resolution = "";
//            if (res != null) {//OneDay
//                resolution = "\"resolution\":\""+res+"\",";
//            }
//            String aggregate = "";
////            String aggregate = "Avg";
////            String aggregate = "Dif";
//            writer.write("{\"queryItems\":[{\"componentId\":\"Plant:1\",\"channelId\":\"Measurement.Metering.GridMs.TotWhIn.ChaSta\",\"timezone\":\"Europe/Brussels\","+resolution+"\"aggregate\":\""+aggregate+"\",\"multiAggregate\":\"Sum\"}],\"dateTimeBegin\":\""+startTime.withZoneSameInstant(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)+"\",\"dateTimeEnd\":\""+endTime.withZoneSameInstant(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)+"\"}");
//            writer.flush();
//
//            if (conn.getResponseCode() == 401) {
//                token = null;
//                throw new RuntimeException();
//            }
//            String body = new String(conn.getInputStream().readAllBytes());
//            conn.getInputStream().close();
//            JsonArray array = new JsonArray(body);
//            JsonArray values = array.getJsonObject(0).getJsonArray("values");
//            List<MeterReading> list = new LinkedList<>();
//            for(int i = 0; i < values.size(); i++) {
//                if (values.getJsonObject(i).containsKey("value")) {
//                    list.add(new MeterReading(ZonedDateTime.parse(values.getJsonObject(i).getString("time")),
//                            values.getJsonObject(i).getInteger("value") / 4 * 4));
//                }
//            }
//            return list;
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public long getConsumptionMeterReading() {
        return getReadingByKey("Measurement.Metering.GridMs.TotWhIn.ChaSta");
    }

    public long getLivePowerMeterReading() {
        return getReadingByKey("Measurement.Metering.GridMs.TotWIn.ChaSta");
    }

    private long getReadingByKey(String key) {
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
                throw new RuntimeException("auth error");
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
        } catch (RuntimeException e) {
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
