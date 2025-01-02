package homecontrol.impl.tesla;

import homecontrol.services.ev.EVState;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TeslaClient {
    public static final Logger LOGGER = Logger.getLogger(TeslaClient.class.getName());

    private static Object commandLock = new Object();

    public static final String BASE = "https://owner-api.teslamotors.com";

    private String refreshToken;
    private String vehicle;
    private String accessToken;
    private String vin;
    private String keyName;
    private String tokenName;
    private String sdkDir;
    private String cacheFile;

    public TeslaClient(String refreshToken, String vehicle, String vin, String keyName, String tokenName, String sdkDir, String cacheFile) {
        this.refreshToken = refreshToken;
        this.vehicle = vehicle;
        this.vin = vin;
        this.keyName = keyName;
        this.tokenName = tokenName;
        this.sdkDir = sdkDir;
        this.cacheFile = cacheFile;
    }

    public EVState getChargeState() throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(BASE + "/api/1/vehicles/"+vehicle+"/vehicle_data"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonObject responseObject = sendRequest(request);
        JsonObject rsp = responseObject.getJsonObject("response").getJsonObject("charge_state");

        EVState state = new EVState();
        state.setTimestamp(Instant.now());
        state.setCharge_amps(rsp.getInteger("charge_amps"));
        state.setCharge_current_request(rsp.getInteger("charge_current_request"));
        state.setCharge_current_request_max(rsp.getInteger("charge_current_request_max"));
        state.setCharger_actual_current(rsp.getInteger("charger_actual_current"));
        state.setCharging_state(rsp.getString("charging_state"));
        state.setCharge_limit_soc(rsp.getInteger("charge_limit_soc"));
        state.setBattery_level(rsp.getInteger("battery_level"));
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
//        if (accessToken == null) {
//            accessToken = getAccessToken(refreshToken);
//        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .timeout(Duration.ofSeconds(10))
//                .uri(URI.create(BASE + "/api/1/vehicles/"+vehicle+"/wake_up"))
//                .header("Accept", "application/json")
//                .header("Authorization", "Bearer " + accessToken)
//                .POST(HttpRequest.BodyPublishers.ofString("{}"))
//                .build();
//        JsonObject responseObject = sendRequest(request);
//        LOGGER.log(Level.INFO, "wake up. result = {0}", responseObject);
    }

    public boolean stopCharging() throws TeslaException {
        executeCommand("charging-stop", null);
        return true;
    }

    public boolean startCharging() throws TeslaException {
        executeCommand("charging-start", null);
        return true;
    }

    private void executeCommand(String command, String opt) throws TeslaException {
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
                LOGGER.info("command " + command + " " + (opt != null ? opt + " " : "") + " executed successfully");
            } catch (TeslaException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "unexpected exception running command", e);
                throw new TeslaException(0, "unexpected exception running command " + e);
            }
        }
    }

    String getAccessToken(String refreshToken) throws TeslaException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("client_id", "ownerapi");
        jsonObject.put("refresh_token", refreshToken);
        jsonObject.put("grant_type", "refresh_token");
        jsonObject.put("scope", "openid email offline_access");

        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create("https://auth.tesla.com/oauth2/v3/token"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();
        JsonObject responseObject = sendRequest(request);

        LOGGER.log(Level.INFO, "retrieved new access token");
        return responseObject.getString("access_token");
    }

    public void clearAccessToken() {
        this.accessToken = null;
    }

    private JsonObject sendRequest(HttpRequest request) throws TeslaException {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new TeslaException(0, e.toString());
        }
        if (response.statusCode() != 200) {
            throw new TeslaException(response.statusCode(), response.body());
        }
        JsonObject responseObject = new JsonObject(response.body());
        return responseObject;
    }


    public void getVehicles() throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }
        System.out.println("accessToken = " + accessToken);
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(BASE + "/api/1/vehicles"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonObject responseObject = sendRequest(request);
        System.out.println(responseObject);
    }

    public JsonObject getVehicle(String id) throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(BASE + "/api/1/vehicles/"+id))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonObject responseObject = sendRequest(request);
        return responseObject;
    }

}
