package homecontrol.impl.sma;

import homecontrol.services.ev.Charger;
import homecontrol.services.ev.StateRefresh;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SMACharger implements Charger {
    public static final Logger LOGGER = Logger.getLogger(SMACharger.class.getName());

    @ConfigProperty(name = "EVCHARGING_CHARGER_IP")
    String chargerIp;
    @ConfigProperty(name = "EVCHARGING_CHARGER_USERNAME")
    String chargerUserName;
    @ConfigProperty(name = "EVCHARGING_CHARGER_PASSWORD")
    String chargerPassword;
    @ConfigProperty(name = "EVCHARGING_CONSUMPTIONMETER_OFFSET", defaultValue = "0")
    long consumptionMeterOffset;

    private final ReentrantLock requestLock = new ReentrantLock();

    private String token;

    private State state;

    private int activePower;


    @PreDestroy
    public void preDestroy() {
        LOGGER.info("logging out SMA charger");
        logout();
    }

    private void logout() {
        if (token != null) {
            try {
                HttpsURLConnection conn = HttpHelper.openConnection("https://"+ chargerIp +"/api/v1/refreshtoken?refreshToken=" + token);
                try {
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestMethod("DELETE");

                    BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
                    LOGGER.info("logout response = " + conn.getResponseCode()+" "+ new String(reader.readAllBytes()));

                    token = null;
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @Retry(maxRetries = 1, delay = 2, delayUnit = ChronoUnit.SECONDS)
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
    @Retry(maxRetries = 1, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public Uni<Integer> getActivePower() {
        int activePower = (int) getLivePowerMeterReading();
        this.activePower = activePower;
        return Uni.createFrom().item(activePower);
    }

    @Override
    public int getLastKnownActivePower() {
        return activePower;
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public int getChargingMeterReading() {
        return (int) getConsumptionMeterReading();
    }

    public void startCharging() {
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4718\"}]}");
    }

    @Override
    public void stopCharging() {
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4721\"}]}");
    }

    @Override
    public void changeChargingAmps(int amps) {
        int powerW = amps * 230;
        if (powerW < 1380) {
            powerW = 1380;
        }
        if (amps == 32 || powerW > 7400) {
            powerW = 7400;
        }
        LOGGER.log(Level.INFO, "change amps to "+amps+"A converted to "+powerW+"W");
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4718\"},{\"channelId\":\"Parameter.Inverter.WMaxIn\",\"timestamp\":\""+d+"\",\"value\":"+powerW+"}]}");
    }

    private void put(String body) {
        requestLock.lock();
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/parameters/IGULD:SELF");
            try {
                conn.setDoOutput(true);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Sec-Fetch-Dest", "empty");
                conn.setRequestProperty("Sec-Fetch-Mode", "cors");
                conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
                conn.setRequestProperty("User-Agent", "homecontrol");

                LOGGER.info("calling SMA charger PUT /api/v1/parameters/IGULD:SELF "+body);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write(body);
                writer.flush();


                if (conn.getResponseCode() == 401) {
                    token = null;
                    throw new RuntimeException("auth error");
                }
                if (conn.getResponseCode() == 204) {
                    return;//OK
                }

                body = new String(conn.getInputStream().readAllBytes());
                conn.getInputStream().close();
                JsonObject object = new JsonObject(body);

                if (object.getString("err") != null) {
                    throw new RuntimeException("error from charger " + object.getString("err"));
                }
            } finally {
                conn.disconnect();
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            requestLock.unlock();
        }
    }

    public State getStateInternal() {
        requestLock.lock();
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/widgets/emobility?componentId=Plant:1");
            try {
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                LOGGER.info("calling SMA charger /api/v1/widgets/emobility?componentId=Plant:1");

                if (conn.getResponseCode() == 401) {
                    token = null;
                    throw new RuntimeException("auth error");
                }
                String body = new String(conn.getInputStream().readAllBytes());
                conn.getInputStream().close();
                JsonObject object = new JsonObject(body);

                if (object.getString("err") != null) {
                    throw new RuntimeException("error from charger " + object.getString("err"));
                }

                State status = State.valueOf(object.getString("chargeStatus"));
                return status;
            } finally {
                conn.disconnect();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            requestLock.unlock();
        }
    }

    public long getConsumptionMeterReading() {
        return getReadingByKey("Measurement.Metering.GridMs.TotWhIn.ChaSta") + consumptionMeterOffset;
    }

    public long getLivePowerMeterReading() {
        return getReadingByKey("Measurement.Metering.GridMs.TotWIn.ChaSta");
    }

    private long getReadingByKey(String key) {
        requestLock.lock();
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/measurements/live/");
            try {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                LOGGER.info("calling SMA charger /api/v1/measurements/live for key "+key);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write("[{\"componentId\":\"Plant:1\"}]");
                writer.flush();

                if (conn.getResponseCode() == 401) {
                    LOGGER.info("SMA charger HTTP 401 result. setting token to null before trying again");
                    token = null;
                    throw new RuntimeException("auth error");
                }
                String body = new String(conn.getInputStream().readAllBytes());
                conn.getInputStream().close();
                JsonArray array = new JsonArray(body);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.getJsonObject(i);
                    if (obj.getString("channelId").equals(key)) {
                        return obj.getJsonArray("values").getJsonObject(0).getLong("value");
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            requestLock.unlock();
        }
        return 0;
    }

    private String authenticate() {
        try {
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/token");
            try {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                LOGGER.info("calling SMA charger /api/v1/token");

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write("grant_type=password&username=" + chargerUserName + "&password=" + chargerPassword);
                writer.flush();

                String body = new String(conn.getInputStream().readAllBytes());
                conn.getInputStream().close();

                JsonObject obj = new JsonObject(body);
                return obj.getString("access_token");
            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
