package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.config.ConfigService;
import homecontrol.services.ev.*;
import homecontrol.services.notications.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EVControlService {
    public static final Logger LOGGER = Logger.getLogger(EVControlService.class.getName());

    @Inject
    @Any
    Instance<ElectricVehicle> allVehicles;
    @Inject
    private NotificationService notificationService;

    @Inject
    private MetricsLogger metricsLogger;
    @Inject
    private ConfigService configService;

    @Inject
    private Charger charger;

    private Charger.State storedChargerState;
    private ElectricVehicle connectedVehicle;
    private Integer currentPowerA;

    public ElectricVehicle getConnectedVehicle() {
        return connectedVehicle;
    }

    public EVState getCurrentState(StateRefresh refresh) throws EVException {
        if (connectedVehicle == null) {
            //no connected vehicle - a 3rd vehicle must be charging
            return null;
        }
        return connectedVehicle.getCurrentState(refresh);
    }

    private void detectConnectedVehicle() {
        ElectricVehicle selected = null;
        for(ElectricVehicle ev : allVehicles) {
            if (ev.isConfigured()) {
                try {
                    EVState teslaState = ev.getCurrentState(StateRefresh.REFRESH_ALWAYS);
                    if (!teslaState.getCharging_state().equalsIgnoreCase("Disconnected")) {
                        if (selected != null && selected.getName().equalsIgnoreCase("Volvo")) {
                            //prefer volvo
                            notificationService.sendNotification("Detect Vehicle Conflict: More than 1 vehicles connected. Selecting Volvo.");
                        } else {
                            selected = ev;
                        }
                    }
                } catch (EVException e) {
                    notificationService.sendNotification("Detect Vehicle: error contacting "+ev.getName()+" "+e);
                }
            }
        }

        if (selected != null) {
            LOGGER.info("detected vehicle: "+selected.getName());
            metricsLogger.logEVCharging("VEHICLE_DETECTED_"+selected.getName().toUpperCase(), 0);
            connectedVehicle = selected;
        }
    }

    public boolean handleChargerState(Charger.State newState) {
        //TODO tests
        if (this.storedChargerState != null && !this.storedChargerState.equals(newState)) {
            LOGGER.info("charger state changed to "+newState);
            if (newState.equals(Charger.State.NotConnected)) {
                //not connected anymore
                LOGGER.info("disable charging so it does not start automatically on next plug-in");
                stopCharging();
                connectedVehicle = null;
            } else if (storedChargerState.equals(Charger.State.NotConnected)) {
                //previously not connected, so have to detect vehicle
                detectConnectedVehicle();
            }
            this.storedChargerState = newState;
            return true;
        } else if (storedChargerState == null) {
            if (!newState.equals(Charger.State.NotConnected)) {
                detectConnectedVehicle();
            }
        }
        this.storedChargerState = newState;
        return false;
    }

    public void changeCharging(int powerA) {
        boolean haveToCharge = powerA >= configService.getMinimumChargingA();
        LOGGER.log(Level.INFO,"haveToCharge=" + haveToCharge+ " currentPowerA="+currentPowerA+" powerA="+powerA+" chargetState="+ storedChargerState);
        if (haveToCharge) {
            if (currentPowerA == null || powerA != currentPowerA.intValue()) {
                metricsLogger.logEVCharging("CHANGEAMPS", powerA);
                charger.changeChargingAmps(powerA);
                currentPowerA = powerA;
            }
        } else {
            if (storedChargerState.equals(Charger.State.InProgress)) {
                stopCharging();
            }
        }
    }

    private void stopCharging() {
        charger.stopCharging();
        currentPowerA = null;
        metricsLogger.logEVCharging("STOP", 0);
    }

}
