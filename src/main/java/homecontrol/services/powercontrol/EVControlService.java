package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.ev.*;
import homecontrol.services.notications.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EVControlService {
    public static final Logger LOGGER = Logger.getLogger(EVControlService.class.getName());

    @Inject
    private ElectricVehicle electricVehicle;
    @Inject
    private NotificationService notificationService;

    @Inject
    private MetricsLogger metricsLogger;

    private Charger.State chargerState;

    public EVState getCurrentState(StateRefresh refresh) throws EVException {
        return electricVehicle.getCurrentState(refresh);
    }

    public boolean handleChargerState(Charger.State state) {
        //TODO tests
        //TODO if charger is used by multiple vehicles, extra checks are needed to make sure the vehicle is only controlled when actually connected to the charger
        if (this.chargerState != null && !this.chargerState.equals(state)) {
            LOGGER.info("charger state changed "+state);
            Boolean enable = null;
            if (state.equals(Charger.State.NotConnected)) {
                enable = Boolean.TRUE;
            } else {
                if (this.chargerState == null || this.chargerState.equals(Charger.State.NotConnected)) {
                    enable = Boolean.FALSE;
                }
            }
            if (enable != null) {
                try {
                    LOGGER.info((enable ? "re-enable" : "disable") + " scheduled charging");
                    if (enable) {
                        electricVehicle.enableScheduledCharging();
                    } else {
                        electricVehicle.disableScheduledCharging();
                    }
                    electricVehicle.stopCharging();
                } catch (EVException e) {
                    LOGGER.severe("could not change scheduled charging to " + enable);
                    notificationService.sendNotification("could not change scheduled charging to " + enable);
                }
                this.chargerState = state;
                return true;
            }
        }
        this.chargerState = state;
        return false;
    }

    public void changeCharging(int powerA) {
        try {
            EVState chargeState = electricVehicle.getCurrentState(StateRefresh.CACHED);

            int currentAmps = chargeState.getCharge_amps();

            if (currentAmps == powerA) {
                LOGGER.log(Level.INFO, "no power difference. leave everything as it is.");
                return;
            }

            int maxAmps = chargeState.getCharge_current_request_max();
            boolean currentlyCharging = chargeState.getCharging_state().equals("Charging")
                    || chargeState.getCharging_state().equals("Starting")
                    || (chargerState != null && chargerState.equals(Charger.State.InProgress));

            boolean haveToCharge = false;

            LOGGER.log(Level.INFO, "Tesla charging state {0}\n" +
                    "\tCurrent amps: {1}A\n" +
                    "\tBattery level: {2}%\n" +
                    "\tCharge limit soc: {3}%\n" +
                    "\tMax amps: {4}\n" +
                    "\tCurrently charging: {5}\n" +
                    "\tCharge amps: {6}A", new Object[]{
                    chargeState.getCharging_state(),
                    currentAmps,
                    chargeState.getBattery_level(),
                    chargeState.getCharge_limit_soc(),
                    maxAmps,
                    currentlyCharging,
                    powerA});

            haveToCharge = powerA >= 5;
            powerA = Math.min(powerA, chargeState.getCharge_current_request_max());

            String action = null;
            if (haveToCharge) {
                LOGGER.log(Level.INFO, "Tesla has to charge at {0}A", powerA);
                if (!currentlyCharging) {
                    currentAmps = 0;
                    electricVehicle.startCharging();
                    action = "STARTED";
                }
                if (currentAmps != powerA) {
                    electricVehicle.changeChargingAmps(powerA);
                    chargeState = electricVehicle.getCurrentState(StateRefresh.REFRESH_ALWAYS);
                    if (action == null) {
                        action = "CHANGE_AMPS";
                    }
                }
            } else {
                LOGGER.log(Level.INFO, "Tesla does not have to charge");
                if (currentlyCharging) {
                    action = "STOPPED";
                    electricVehicle.stopCharging();
                    electricVehicle.changeChargingAmps(5);
                    chargeState = electricVehicle.getCurrentState(StateRefresh.REFRESH_ALWAYS);
                }
            }
            if (action != null) {
                metricsLogger.logEVCharging(action, powerA);
            }
        } catch (EVException e) {
            notificationService.sendNotification("could not contact tesla "+e);
            throw new RuntimeException(e);
        }
    }

}
