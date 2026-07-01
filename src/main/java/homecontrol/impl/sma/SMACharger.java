package homecontrol.impl.sma;

import homecontrol.services.ev.Charger;
import homecontrol.services.ev.StateRefresh;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private int activePowerW;
    private int currentChargingAmps;

    private final HttpClient http;

    public SMACharger() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1) // simple embedded server
                .build();
    }

    @PreDestroy
    public void preDestroy() {
        http.close();
        LOGGER.info("logging out SMA charger");
        logout();
    }

    /**
     * Performs one SEMP read of state + active power.
     *
     * <p>Thread-safe: {@link HttpClient} is thread-safe and this method keeps no
     * mutable state, so the single bean instance can be called concurrently from
     * schedulers and REST endpoints. Checked exceptions are wrapped as unchecked
     * so callers (e.g. a {@code @Scheduled} poller) don't have to declare them.
     */
    public BigDecimal readActivePower() {
        try {
            Document doc = parse(httpGet("http://" + chargerIp + "/SEMP/Parameters"));
            String raw = parameterValue(doc, "Measurement.Metering.GridMs.TotWIn.ChaSta");
            return parseDouble(raw);
        } catch (IOException e) {
            throw new UncheckedIOException("SEMP read failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SEMP read interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("SEMP parse failed", e);
        }
    }

    /** Single GET, full-body read (connection returns to the pool — nothing to close). */
    private byte[] httpGet(String target) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(target))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/xml")
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " from " + target);
        }
        return resp.body();
    }

    /**
     * Reads the granular charging-session state from {@code /SEMP/Parameters}.
     * This is a second (still unauthenticated) GET, so poll it on its own cadence
     * — session state changes far less often than active power.
     */
    public Charger.State readChargeStateSEMP() {
        try {
            Document doc = parse(httpGet("http://" + chargerIp + "/SEMP/Parameters"));
            String raw = parameterValue(doc, "Measurement.Operation.EVeh.ChaStt");
            if (raw.isEmpty()) {
                throw new IllegalStateException("ChaStt channel not present in /SEMP/Parameters");
            }
            return getState(Integer.parseInt(raw.trim()));
        } catch (IOException e) {
            throw new UncheckedIOException("SEMP parameters read failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SEMP parameters read interrupted", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SEMP parameters parse failed", e);
        }
    }

    private Charger.State getState(int tag) {
        return switch (tag) {
            case 200111 -> State.NotConnected;
            case 200112 -> State.Waiting;
            case 200113 -> State.InProgress;
            default -> null;
        };
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        trySetFeature(dbf, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(dbf, "http://xml.org/sax/features/external-general-entities", false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xml));
    }

    private static String firstText(Document doc, String tag) {
        return firstText(doc.getDocumentElement(), tag);
    }

    private static String firstText(Element scope, String tag) {
        if (scope == null) return "";
        NodeList nl = scope.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        String t = nl.item(0).getTextContent();
        return (t == null) ? "" : t.trim();
    }

    /** Returns the {@code <value>} of the {@code <Parameter>} matching channelId, or "". */
    private static String parameterValue(Document doc, String channelId) {
        NodeList params = doc.getElementsByTagName("Parameter");
        for (int i = 0; i < params.getLength(); i++) {
            Element p = (Element) params.item(i);
            if (channelId.equals(firstText(p, "channelId"))) {
                return firstText(p, "value");
            }
        }
        return "";
    }

    private static BigDecimal parseDouble(String s) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(s.trim()));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static void trySetFeature(DocumentBuilderFactory dbf, String f, boolean v) {
        try {
            dbf.setFeature(f, v);
        } catch (Exception ignored) {
        }
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
    @CacheResult(cacheName = "sma-charger-power")
    public Uni<Integer> getActivePowerW() {
        int activePower = readActivePower().intValue();
        this.activePowerW = activePower;
        return Uni.createFrom().item(activePower);
    }

    @Override
    public int getCurrentChargingAmps() {
        return this.currentChargingAmps;
    }

    @Override
    public int getLastKnownActivePower() {
        return activePowerW;
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public int getChargingMeterReading() {
        return (int) getConsumptionMeterReading();
    }

    @Retry(maxRetries = 1, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public void startCharging() {
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4718\"}]}");
    }

    @Override
    @Retry(maxRetries = 1, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public void stopCharging() {
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4721\"}]}");
        this.currentChargingAmps = 0;
    }

    @Override
    @Retry(maxRetries = 1, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public void changeChargingAmps(int amps, int voltage) {
        int powerW = amps * voltage;
        LOGGER.fine("voltage = "+voltage+" amps = "+amps+" power = "+powerW);
        if (powerW < 1380) {
            powerW = 1380;
        }
        if (amps == 32 || powerW > 7400) {
            powerW = 7400;
        }
        LOGGER.log(Level.INFO, "change amps to "+amps+"A converted to "+powerW+"W");
        String d = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'").format(ZonedDateTime.now(ZoneId.of("UTC")));
        put("{\"values\":[{\"channelId\":\"Parameter.Chrg.ActChaMod\",\"timestamp\":\""+d+"\",\"value\":\"4718\"},{\"channelId\":\"Parameter.Inverter.WMaxIn\",\"timestamp\":\""+d+"\",\"value\":"+powerW+"}]}");
        this.currentChargingAmps = amps;
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
            return readChargeStateSEMP();
        } finally {
            requestLock.unlock();
        }
    }

    public long getConsumptionMeterReading() {
        return getReadingByKey("Measurement.Metering.GridMs.TotWhIn.ChaSta") + consumptionMeterOffset;
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
