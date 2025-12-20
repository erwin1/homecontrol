package homecontrol.impl.volvo;

import homecontrol.services.ev.EVException;
import homecontrol.services.ev.EVState;
import homecontrol.services.ev.ElectricVehicle;
import homecontrol.services.ev.StateRefresh;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ApplicationScoped
@Named("volvo")
public class VolvoEV implements ElectricVehicle {
    @ConfigProperty(name = "EVCHARGING_VOLVO_NAME")
    String name;
    @ConfigProperty(name = "EVCHARGING_VOLVO_APIKEY")
    String apiKey;
    @ConfigProperty(name = "EVCHARGING_VOLVO_VEHICLE_VIN")
    Optional<String> vehicleVIN;
    @Inject
    private VolvoTokenService volvoTokenService;
    private EVState currentState;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isConfigured() {
        return vehicleVIN.isPresent();
    }

    @Override
    public EVState getCurrentState(StateRefresh stateRefresh) throws EVException {
        EVState state = switch (stateRefresh) {
            case CACHED_OR_NULL -> currentState;
            case CACHED -> getCurrentState(stateRefresh.getMaxCacheTimeIfOnline(), stateRefresh.getMaxCacheTime());
            case REFRESH_IF_ONLINE -> doGetCurrentState();
            case REFRESH_ALWAYS -> doGetCurrentState();
        };
        if (state != null) {
            currentState = state;
        }
        return state;
    }

    private EVState getCurrentState(Duration maxCacheTimeIfOnline, Duration maxCacheTime) throws EVException {
        if (currentState == null) {
            return doGetCurrentState();
        }
        if (currentState.getTimestamp().plus(maxCacheTime.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now())) {
            return doGetCurrentState();
        }
        if (currentState.getTimestamp().plus(maxCacheTimeIfOnline.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now())) {
            EVState newState = doGetCurrentState();
            if (newState != null) {
                return newState;
            }
        }
        return currentState;
    }

    public EVState doGetCurrentState() throws EVException {
        String accessToken = volvoTokenService.getAccessToken();
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create("https://api.volvocars.com/energy/v2/vehicles/"+vehicleVIN.get()+"/state"))
                .header("vcc-api-key", apiKey)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonObject responseObject = null;
        try {
            responseObject = sendRequest(request);
        } catch (VolvoException e) {
            throw new EVException(e);
        }
        int batteryLevel = responseObject.getJsonObject("batteryChargeLevel").getNumber("value").intValue();
        String updatedAt = responseObject.getJsonObject("batteryChargeLevel").getString("updatedAt");
        ZonedDateTime updatedAtDT = ZonedDateTime.parse(updatedAt);
        //chargerConnectionStatus CONNECTED DISCONNECTED FAULT
        String chargerConnectionStatus = responseObject.getJsonObject("chargerConnectionStatus").getString("value");
        //chargingStatus IDLE CHARGING SCHEDULED DISCHARGING ERROR DONE
        String chargingStatus = responseObject.getJsonObject("chargingStatus").getString("value");
        int chargingPower = responseObject.getJsonObject("chargingPower").getNumber("value").intValue();
        int chargingCurrentLimit = responseObject.getJsonObject("chargingCurrentLimit").getNumber("value").intValue();
        int targetBatteryChargeLevel = responseObject.getJsonObject("targetBatteryChargeLevel").getNumber("value").intValue();

        EVState evState = new EVState();
        evState.setTimestamp(updatedAtDT.toInstant());
        evState.setBattery_level(batteryLevel);
        evState.setCharging_state(chargerConnectionStatus.equalsIgnoreCase("DISCONNECTED") ? chargerConnectionStatus : chargingStatus);
        evState.setCharge_amps(chargingPower/230);
        evState.setCharge_limit_soc(targetBatteryChargeLevel);
        evState.setCharge_current_request_max(chargingCurrentLimit);
        evState.setCharger_power(chargingPower);
        return evState;
    }

    @Override
    public Uni<Boolean> openChargePortDoor() throws EVException {
        throw new EVException("not supported in Volvo EV");
    }

    @Override
    public void debug() {
        String accessToken = volvoTokenService.getAccessToken();
        System.out.println("accessToken = " + accessToken);
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create("https://api.volvocars.com/connected-vehicle/v2/vehicles"))
                .header("vcc-api-key", apiKey)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonObject responseObject = null;
        try {
            responseObject = sendRequest(request);
        } catch (VolvoException e) {
            throw new RuntimeException(e);
        }
        System.out.println(responseObject);
    }

    private JsonObject sendRequest(HttpRequest request) throws VolvoException {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new VolvoException(0, e.toString());
        }
        if (response.statusCode() != 200) {
            throw new VolvoException(response.statusCode(), response.body());
        }
        JsonObject responseObject = new JsonObject(response.body());
        return responseObject;
    }
}
