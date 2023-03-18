package evcharging.impl.tesla;

import evcharging.services.NotificationService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TeslaService {
    public static final Logger LOGGER = Logger.getLogger(TeslaService.class.getName());

    @ConfigProperty(name = "EVCHARING_TESLA_REFRESHTOKEN")
    String refreshToken;

    @ConfigProperty(name = "EVCHARING_TESLA_VEHICLE")
    String vehicle;
    private TeslaClient teslaClient;

    @Inject
    NotificationService notificationService;

    @PostConstruct
    public void postConstruct() {
        teslaClient = new TeslaClient(refreshToken, vehicle);
    }

    @Retry(maxRetries = 3, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public ChargeState getChargeState() throws TeslaException {
        LOGGER.info("in getChargeState()");
        try {
            return teslaClient.getChargeState();
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

    @Retry(maxRetries = 3, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public boolean setChargingAmps(int amps) throws TeslaException {
        try {
            return teslaClient.setChargingAmps(amps);
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

    @Retry(maxRetries = 10, delay = 30, delayUnit = ChronoUnit.SECONDS)
    public boolean setScheduledCharging(boolean enabled, int time) throws TeslaException {
        try {
            return teslaClient.setScheduledCharging(enabled, time);
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

    @Retry(maxRetries = 3, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public boolean wakeup() throws TeslaException {
        try {
            return teslaClient.wakeup();
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

    @Retry(maxRetries = 3, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public boolean stopCharging() throws TeslaException {
        try {
            return teslaClient.stopCharging();
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

    @Retry(maxRetries = 3, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public boolean startCharging() throws TeslaException {
        try {
            return teslaClient.startCharging();
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw e;
        }
    }

        private void handleTeslaException(TeslaException e) {
        if (e.getCode() == 401) {
            teslaClient.clearAccessToken();
        } else if (e.getCode() == 408) {
            try {
                LOGGER.info("trying to wake-up tesla");
                teslaClient.wakeup();
            } catch (TeslaException ex) {
                LOGGER.log(Level.SEVERE, "error in tesla wakeup", ex);
                notificationService.sendNotification("error in tesla wake-up");
            }
        } else {
            LOGGER.log(Level.SEVERE, "unexpected error in tesla call", e);
            notificationService.sendNotification("unexpected error in tesla call "+e);
        }
    }
}
