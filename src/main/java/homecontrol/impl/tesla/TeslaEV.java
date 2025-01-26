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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TeslaEV implements ElectricVehicle {
    public static final Logger LOGGER = Logger.getLogger(TeslaEV.class.getName());

    @Inject
    private MetricsLogger metricsLogger;

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
    private TeslaClient teslaClient;
    private EVState currentState;

    @PostConstruct
    public void postConstruct() {
        teslaClient = new TeslaClient(vin, keyName, tokenName, sdkDir, cacheFile);
    }

    @Override
    @Retry(maxRetries = 3, delay = 15, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public EVState getCurrentState(StateRefresh stateRefresh) throws EVException {
        LOGGER.fine("getCurrentState "+stateRefresh);
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
            EVState state = teslaClient.getChargeState();
            metricsLogger.logEVCharging("REFRESHED_STATE", state.getCharge_amps());
            return state;
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    private EVState refreshCurrentStateIfOnline() throws EVException {
        LOGGER.fine("refreshCurrentStateIfOnline");
        try {
            if (isVehicleOnline()) {
                LOGGER.fine("vehicle is online");
                EVState state = teslaClient.getChargeState();
                metricsLogger.logEVCharging("REFRESHED_STATE_ONLINE", state.getCharge_amps());
                return state;
            }
        } catch (TeslaException e) {
            LOGGER.info("non-fatal exception while getting charge state "+e);
        }
        return null;
    }

    private void invalidateAndRefreshCacheIfOnline() {
        try {
            currentState = null;
            refreshCurrentStateIfOnline();
        }catch (EVException e) {
            LOGGER.info("invalidated cache but could not refresh state.");
        }
    }

    @Retry(maxRetries = 2, delay = 2, delayUnit = ChronoUnit.SECONDS)
    @Override
    public boolean isVehicleOnline() throws EVException {
        try {
            LOGGER.fine("checking if vehicle is online");
            return teslaClient.isVehicleOnline();
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }


    @Override
    @Retry(maxRetries = 2, delay = 30, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void startCharging() throws EVException {
        try {
            teslaClient.startCharging();
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
        invalidateAndRefreshCacheIfOnline();
    }

    @Override
    @Retry(maxRetries = 2, delay = 30, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void stopCharging() throws EVException {
        try {
            teslaClient.stopCharging();
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
        invalidateAndRefreshCacheIfOnline();
    }

    @Override
    @Retry(maxRetries = 2, delay = 30, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void changeChargingAmps(int amps) throws EVException {
        try {
            teslaClient.setChargingAmps(amps);
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
        invalidateAndRefreshCacheIfOnline();
    }

    @Override
    @Retry(maxRetries = 3, delay = 15, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void enableScheduledCharging() throws EVException {
        try {
            teslaClient.setScheduledCharging(true, 1320);
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    @Override
    @Retry(maxRetries = 3, delay = 15, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    public void disableScheduledCharging() throws EVException {
        try {
            teslaClient.setScheduledCharging(false, 1320);
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    @Override
    @Retry(maxRetries = 8, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = EVException.class)
    @Asynchronous
    public Uni<Boolean> openChargePortDoor() throws EVException {
        try {
            boolean result = teslaClient.openChargePortDoor();
            return Uni.createFrom().item(result);
        } catch (TeslaException e) {
            throw handleTeslaException(e);
        }
    }

    private EVException handleTeslaException(TeslaException e) {
        if (e.getCode() == 408) {
            try {
                LOGGER.info("trying to wake-up tesla");
                teslaClient.wakeup();
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
