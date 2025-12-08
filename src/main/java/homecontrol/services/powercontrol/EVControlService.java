package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.config.ConfigService;
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
    @Inject
    private ConfigService configService;

    @Inject
    private Charger charger;

    private Charger.State chargerState;

    private Integer currentPowerA;

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
                this.chargerState = state;
                return true;
            }
        }
        this.chargerState = state;
        return false;
    }

    public void changeCharging(int powerA) {
        boolean haveToCharge = powerA >= configService.getMinimumChargingA();
        LOGGER.log(Level.INFO,"haveToCharge=" + haveToCharge+ " currentPowerA="+currentPowerA+" powerA="+powerA+" chargetState="+chargerState);
        if (haveToCharge) {
            if (currentPowerA == null || powerA != currentPowerA.intValue()) {
                metricsLogger.logEVCharging("CHANGEAMPS", powerA);
                charger.changeChargingAmps(powerA);
                currentPowerA = powerA;
            }
        } else {
            if (chargerState.equals(Charger.State.InProgress)) {
                charger.stopCharging();
                metricsLogger.logEVCharging("STOP", powerA);
            }
        }
    }

}
