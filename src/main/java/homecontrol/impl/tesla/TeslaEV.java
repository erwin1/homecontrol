package homecontrol.impl.tesla;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.ev.EVException;
import homecontrol.services.ev.EVState;
import homecontrol.services.ev.ElectricVehicle;
import homecontrol.services.ev.StateRefresh;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Named("tesla")
public class TeslaEV implements ElectricVehicle {
    public static final Logger LOGGER = Logger.getLogger(TeslaEV.class.getName());

    @Inject
    private MetricsLogger metricsLogger;

    @ConfigProperty(name = "EVCHARGING_TESLA_NAME")
    String name;
    @ConfigProperty(name = "EVCHARGING_TESLA_VEHICLE_VIN")
    String vin;
    @ConfigProperty(name = "EVCHARGING_TESLA_KEY_NAME")
    String keyName;
    @ConfigProperty(name = "EVCHARGING_TESLA_TOKEN_NAME")
    String tokenName;
    @ConfigProperty(name = "EVCHARGING_TESLA_CACHE_FILE")
    String cacheFile;
    @ConfigProperty(name = "EVCHARGING_TESLA_COMMAND_SDK")
    String sdkDir;
    @ConfigProperty(name = "EVCHARGING_TESLA_REFRESHTOKEN")
    String refreshToken;
    @ConfigProperty(name = "EVCHARGING_TESLA_VEHICLE")
    Optional<String> vehicle;

    private TeslaAPIClient teslaAPIClient;

    private TeslaBLEClient teslaBLEClient;
    private EVState currentState;

    @PostConstruct
    public void postConstruct() {
        if (isConfigured()) {
            teslaBLEClient = new TeslaBLEClient(vin, keyName, tokenName, sdkDir, cacheFile);
            teslaAPIClient = new TeslaAPIClient(refreshToken, vehicle.get());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isConfigured() {
        return vehicle.isPresent();
    }

    @Override
    @Retry(maxRetries = 3, delay = 15, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public EVState getCurrentState(StateRefresh stateRefresh) throws EVException {
        EVState state = switch (stateRefresh) {
            case CACHED_OR_NULL -> currentState;
            case CACHED -> getCurrentState(stateRefresh.getMaxCacheTimeIfOnline(), stateRefresh.getMaxCacheTime());
            case REFRESH_IF_ONLINE -> refreshCurrentStateIfOnline();
            case REFRESH_ALWAYS -> refreshCurrentState();
        };
        if (state != null) {
            currentState = state;
        }
        return state;
    }

    private EVState getCurrentState(Duration maxCacheTimeIfOnline, Duration maxCacheTime) throws EVException {
        if (currentState == null) {
            return refreshCurrentState();
        }
        if (currentState.getTimestamp().plus(maxCacheTime.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now())) {
            return refreshCurrentState();
        }
        if (currentState.getTimestamp().plus(maxCacheTimeIfOnline.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now())) {
            EVState newState = refreshCurrentStateIfOnline();
            if (newState != null) {
                return newState;
            }
        }
        return currentState;
    }

    private EVState refreshCurrentState() throws EVException {
        try {
            LOGGER.fine("refreshing state");
            EVState state = teslaAPIClient.getChargeState();
            metricsLogger.logEVCharging("REFRESHED_STATE", state.getCharge_amps());
            return state;
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    private EVState refreshCurrentStateIfOnline() throws EVException {
        try {
            if (isVehicleOnline()) {
                LOGGER.fine("vehicle is online");
                EVState state = teslaAPIClient.getChargeState();
                metricsLogger.logEVCharging("REFRESHED_STATE_ONLINE", state.getCharge_amps());
                return state;
            }
        } catch (TeslaException e) {
            LOGGER.info("non-fatal exception while getting charge state "+e);
        }
        return null;
    }

    public boolean isVehicleOnline() throws EVException {
        try {
            JsonObject vehicle = teslaAPIClient.getVehicle(this.vehicle.get());
            if (vehicle.getJsonObject("response").getString("state").equals("online")) {
                return true;
            }
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
        return false;
    }

    @Override
    @Retry(maxRetries = 8, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    @Asynchronous
    public Uni<Boolean> openChargePortDoor() throws EVException {
        try {
            boolean result = teslaBLEClient.openChargePortDoor();
            return Uni.createFrom().item(result);
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    @Retry(maxRetries = 3, delay = 15, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void debug() throws EVException {
    }

    private EVException handleTeslaException(TeslaException e) {
        if (e.getCode() == 401) {
            teslaAPIClient.clearAccessToken();
        } else if (e.getCode() == 408) {
            try {
                LOGGER.info("trying to wake-up tesla");
                teslaAPIClient.wakeup();
            } catch (TeslaException ex) {
                LOGGER.log(Level.SEVERE, "error in tesla wakeup", ex);
//                notificationService.sendNotification("error in tesla wake-up");
            }
        } else {
            LOGGER.log(Level.SEVERE, "unexpected error in tesla call", e);
//            notificationService.sendNotification("unexpected error in tesla call "+e);
        }
        return new EVException(e);
    }
}
