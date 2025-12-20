package homecontrol.impl.tesla;

import homecontrol.services.ev.EVState;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeslaAPIClient {
    public static final Logger LOGGER = Logger.getLogger(TeslaBLEClient.class.getName());

    public static final String BASE = "https://owner-api.teslamotors.com";

    private String refreshToken;
    private String vehicle;
    private String accessToken;

    public TeslaAPIClient(String refreshToken, String vehicle) {
        this.refreshToken = refreshToken;
        this.vehicle = vehicle;
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

    public void wakeup() throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(BASE + "/api/1/vehicles/"+vehicle+"/wake_up"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        JsonObject responseObject = sendRequest(request);
        LOGGER.log(Level.INFO, "wake up. result = {0}", responseObject);
    }

    public void clearAccessToken() {
        this.accessToken = null;
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
