package evcharging.impl.tesla;

import evcharging.services.EV;
import evcharging.services.EVCharger;
import evcharging.services.NotificationService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TeslaEV implements EV {
    public static final Logger LOGGER = Logger.getLogger(TeslaEV.class.getName());

    @Inject
    NotificationService notificationService;

    @Inject
    TeslaService teslaService;

    private ChargeState chargeState;
    private EVCharger.State chargerState;


    public void receiveEVChargerStateEvent(@Observes EVCharger.State state) {
        LOGGER.info("received charger state event: "+state);
        Boolean enable = null;
        if (state.equals(EVCharger.State.NotConnected)) {
            enable = Boolean.TRUE;
        } else {
            if (chargerState == null || chargerState.equals(EVCharger.State.NotConnected)) {
                enable = Boolean.FALSE;
            }
        }
        if (enable != null) {
            try {
                LOGGER.info((enable ? "re-enable" : "disable")+" scheduled charging");
                teslaService.setScheduledCharging(enable, 1320);
                this.chargeState = teslaService.getChargeState();
            } catch (TeslaException e) {
                LOGGER.severe("could not change scheduled charging to "+enable);
                notificationService.sendNotification("could not change scheduled charging to "+enable);
            }
        }
        chargerState = state;
    }

    @Override
    public boolean isChargingComplete() {
        try {
            if (chargeState == null) {
                chargeState = teslaService.getChargeState();
            }
            return chargeState.getBattery_level() >= chargeState.getCharge_limit_soc();
        } catch (TeslaException e) {
            notificationService.sendNotification("could not get tesla charge state "+e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCurrentBatteryLevel() {
        try {
            if (chargeState == null) {
                chargeState = teslaService.getChargeState();
            }
            return chargeState.getBattery_level();
        } catch (TeslaException e) {
            notificationService.sendNotification("could not get tesla charge state "+e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void requestPowerConsumptionChange(int powerW, Integer limit) {
        try {
            int powerDiffA = powerW / 220;
            if (powerDiffA == 0) {
                LOGGER.log(Level.FINE, "no power difference. leave everything as it is.");
                return;
            }

            if (chargeState == null) {
                chargeState = teslaService.getChargeState();
            }
            int currentAmps = chargeState.getCharge_amps();
            int maxAmps = chargeState.getCharge_current_request_max();
            boolean currentlyCharging = chargeState.getCharging_state().equals("Charging");

            LOGGER.log(Level.INFO, "Tesla charging state {0} ({1}A). Battery level {2}%", new Object[]{
                    chargeState.getCharging_state(),
                    currentAmps,
                    chargeState.getBattery_level()});

            boolean haveToCharge = false;
            int chargeAmps = (currentlyCharging ? currentAmps : 0) + powerDiffA;

            if (limit == null || limit > chargeState.getCharge_limit_soc()) {
                limit = chargeState.getCharge_limit_soc();
            }

            if (chargeState.getBattery_level() < limit) {
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
                    teslaService.startCharging();
                    notificationService.sendNotification("Started charging at "+chargeAmps+"A");
                }
                if (currentAmps != chargeAmps) {
                    teslaService.setChargingAmps(chargeAmps);
                    chargeState = teslaService.getChargeState();
                }
            } else {
                LOGGER.log(Level.INFO, "Tesla does not have to charge");
                if (currentlyCharging) {
                    teslaService.stopCharging();
                    notificationService.sendNotification("Stopped charging");
                    teslaService.setChargingAmps(5);
                    chargeState = teslaService.getChargeState();
                }
            }
        } catch (TeslaException e) {
            notificationService.sendNotification("could not contact tesla "+e);
            throw new RuntimeException(e);
        }
    }

}
