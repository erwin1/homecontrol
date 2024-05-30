package homecontrol.impl.tesla;

import homecontrol.impl.sma.HttpHelper;
import homecontrol.services.ev.EVState;
import io.vertx.core.json.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeslaClient {
    public static final Logger LOGGER = Logger.getLogger(TeslaClient.class.getName());

    public static final String BASE = "https://fleet-api.prd.eu.vn.cloud.tesla.com";
    public static final String PROXYBASE = "https://localhost:4443";

    private String clientId;
    private String refreshToken;
    private String vehicle;
    private String vehicleVIN;
    private String accessToken;

    public TeslaClient(String clientId, String refreshToken, String vehicle, String vehicleVIN) {
        this.clientId = clientId;
        this.refreshToken = refreshToken;
        this.vehicle = vehicle;
        this.vehicleVIN = vehicleVIN;
    }

    public EVState getChargeState() throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }
        System.out.println("accessToken = " + accessToken);
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

//    public EVState getServiceData() throws TeslaException {
//        if (accessToken == null) {
//            accessToken = getAccessToken(refreshToken);
//        }
//
//        System.out.println("accessToken = " + accessToken);
////
//        String startDate = URLEncoder.encode("2023-11-01T00:00:00+01:00");
//        String endDate = URLEncoder.encode("2023-12-01T00:00:00+01:00");
//
//        Form form = new Form();
//        HttpRequest request = HttpRequest.newBuilder()
//                .timeout(Duration.ofSeconds(10))
//                .uri(URI.create(BASE + "/api/1/vehicles/"+vehicle+"/charge_history?start_date="+startDate+"&end_date="+endDate+"&timezone=Europe/Brussels"))
////                .uri(URI.create("https://fleet-api.prd.eu.vn.cloud.tesla.com/api/1/dx/charging/history"))
//                .header("Accept", "application/json")
//                .header("x-tesla-user-agent", "TeslaApp/4.9.2")
//                .header("Authorization", "Bearer " + accessToken)
//                    .POST(HttpRequest.BodyPublishers.ofString(""))
//                .build();
//        JsonObject responseObject = sendRequest(request);
//        System.out.println("responseObject = " + responseObject);
//        //response.charging_history_graph.data_points.
//        //  .timestamp.timestamp.seconds
//        //  .timestamp.display_string
//        //  .values[1].raw_value
//        JsonArray array = responseObject.getJsonObject("response").getJsonObject("charging_history_graph").getJsonArray("data_points");
//        int total = 0;
//        for(int i = 0; i< array.size(); i++) {
//            JsonArray values = array.getJsonObject(i).getJsonArray("values");
//
//            if (values.getJsonObject(1).containsKey("raw_value")) {
//                int v = values.getJsonObject(1).getInteger("raw_value");
//                System.out.println(v);
//                total += v;
//            }
//        }
//        System.out.println("total = " + total);
//        return null;
//    }

    public boolean openChargePortDoor() throws TeslaException {
        return executeCommand("charge_port_door_open", "{}");
    }

    public boolean flashLights() throws TeslaException {
        return executeCommand("flash_lights", "{}");
    }

    public boolean executeCommand(String command, String body) throws TeslaException {
        if (accessToken == null) {
            accessToken = getAccessToken(refreshToken);
        }

        try {
            HttpsURLConnection conn = HttpHelper.openConnection(PROXYBASE + "/api/1/vehicles/"+vehicleVIN+"/command/"+command);
            conn.setDoOutput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+accessToken);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write(body);
            writer.flush();
            int status = conn.getResponseCode();
            if (status != 200) {
                //500 from proxy could mean http 408
                if (status == 500) {
                    throw new TeslaException(408, "500 status code from tesla. act as 408 error");
                }
                throw new TeslaException(status, "non 200 status code from tesla "+status);
            }
            BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
            JsonObject responseObject = new JsonObject(new String(reader.readAllBytes()));
            boolean result = responseObject.getJsonObject("response").getBoolean("result");
            LOGGER.log(Level.INFO, command+". result = {0}", result);
            return result;
        } catch (IOException e) {
            throw new TeslaException(0, e.toString());
        } catch (RuntimeException | TeslaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean setChargingAmps(int amps) throws TeslaException {
        return executeCommand("set_charging_amps", "{\"charging_amps\":"+amps+"}");
    }

    public boolean setScheduledCharging(boolean enabled, int time) throws TeslaException {
        return executeCommand("set_scheduled_charging", "{\"enable\":"+enabled+", \"time\": "+time+"}");
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

    public boolean stopCharging() throws TeslaException {
        return executeCommand("charge_stop", "{}");
    }

    public boolean startCharging() throws TeslaException {
        return executeCommand("charge_start", "{}");
    }

    String getAccessToken(String refreshToken) throws TeslaException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("client_id", clientId);
        jsonObject.put("refresh_token", refreshToken);
        jsonObject.put("grant_type", "refresh_token");
        jsonObject.put("audience", "https://fleet-api.prd.na.vn.cloud.tesla.com");
        jsonObject.put("scope", "openid vehicle_device_data vehicle_cmds vehicle_charging_cmds");

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
