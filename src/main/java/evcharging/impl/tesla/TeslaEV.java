package evcharging.impl.tesla;

import evcharging.services.EV;
import evcharging.services.NotificationService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TeslaEV implements EV {
    public static final Logger LOGGER = Logger.getLogger(TeslaEV.class.getName());

    @ConfigProperty(name = "EVCHARING_TESLA_REFRESHTOKEN")
    private String refreshToken;

    @ConfigProperty(name = "EVCHARING_TESLA_VEHICLE")
    private String vehicle;

    @Inject
    NotificationService notificationService;

    private TeslaClient teslaClient;
    private LocalDateTime latestWakeupError = null;

    //TODO: check tesla charging planning state and disable it?

    @PostConstruct
    public void postConstruct() {
        teslaClient = new TeslaClient(refreshToken, vehicle);
    }

    @Override
    public boolean isChargingComplete() {
        try {
            //TODO caching to reduce calls?
            ChargeState chargeState = teslaClient.getChargeState();
            return chargeState.getBattery_level() >= chargeState.getCharge_limit_soc();
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void requestPowerConsumptionChange(int powerW) {
        try {
            int powerDiffA = powerW / 220;
            if (powerDiffA == 0) {
                LOGGER.log(Level.FINE, "no power difference. leave everything as it is.");
                return;
            }
            //TODO: caching to reduce calls?
            ChargeState chargeState = teslaClient.getChargeState();
            int currentAmps = chargeState.getCharge_amps();
            int maxAmps = chargeState.getCharge_current_request_max();
            boolean currentlyCharging = chargeState.getCharging_state().equals("Charging");

            LOGGER.log(Level.INFO, "Tesla charging state {0} ({1}A). Battery level {2}%", new Object[]{
                    chargeState.getCharging_state(),
                    currentAmps,
                    chargeState.getBattery_level()});

            boolean haveToCharge = false;
            int chargeAmps = (currentlyCharging ? currentAmps : 0) + powerDiffA;

            if (chargeState.getBattery_level() < chargeState.getCharge_limit_soc()) {
                if (chargeAmps < 5) {
                    haveToCharge = false;
                } else {
                    haveToCharge = true;
                    chargeAmps = Math.min(chargeAmps, maxAmps);
                }
            }

            if (haveToCharge) {
                LOGGER.log(Level.INFO, "Tesla has to charge at {0}A", chargeAmps);
                if (!currentlyCharging) {
                    currentAmps = 0;
                    teslaClient.startCharging();
                    notificationService.sendNotification("Started charging at "+chargeAmps+"A");
                }
                if (currentAmps != chargeAmps) {
                    teslaClient.setChargingAmps(chargeAmps);
                }
            } else {
                LOGGER.log(Level.INFO, "Tesla does not have to charge");
                if (currentlyCharging) {
                    teslaClient.stopCharging();
                    notificationService.sendNotification("Stopped charging");
                    teslaClient.setChargingAmps(5);
                }
            }
        } catch (TeslaException e) {
            handleTeslaException(e);
            throw new RuntimeException(e);
        }
    }

    private void handleTeslaException(TeslaException e) {
        if (e.getCode() == 401) {
            teslaClient.clearAccessToken();
        } else if (e.getCode() == 408) {
            try {
                if (latestWakeupError != null && latestWakeupError.isAfter(LocalDateTime.now().minusMinutes(3))) {
                    notificationService.sendNotification("had to wake up tesla more than once in the last 3 minutes");
                    latestWakeupError = null;
                } else {
                    latestWakeupError = LocalDateTime.now();
                }
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
