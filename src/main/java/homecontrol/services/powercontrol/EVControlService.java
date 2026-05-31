package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.config.ConfigService;
import homecontrol.services.ev.*;
import homecontrol.services.notications.NotificationType;
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
            return null;
        }
        return connectedVehicle.getCurrentState(refresh);
    }

    private void detectConnectedVehicle() {
        ElectricVehicle selected = null;
        for(ElectricVehicle ev : allVehicles) {
            if (ev.isConfigured()) {
                try {
                    EVState evState = ev.getCurrentState(StateRefresh.REFRESH_ALWAYS);
                    if (!evState.getCharging_state().equalsIgnoreCase("Disconnected")) {
                        if (selected != null) {
                            notificationService.sendNotification(NotificationType.ERROR, "Detect Vehicle Conflict: More than 1 vehicle connected. Selected "+selected.getName());
                        } else {
                            selected = ev;
                        }
                    }
                } catch (EVException e) {
                    notificationService.sendNotification(NotificationType.ERROR, "Detect Vehicle: error contacting "+ev.getName()+" "+e);
                }
            }
        }

        if (selected != null) {
            LOGGER.info("detected vehicle: "+selected.getName());
            connectedVehicle = selected;
        } else {
            connectedVehicle = new OtherElectricVehicle();
        }
        metricsLogger.logEVCharging("VEHICLE_DETECTED_"+connectedVehicle.getName().toUpperCase(), 0);
        metricsLogger.logChargerDataPoint(connectedVehicle.getName());
        notificationService.sendNotification(NotificationType.INFO, "Vehicle connected : "+connectedVehicle.getName());

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
                notificationService.sendNotification(NotificationType.INFO, "Vehicle disconnected");
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

    public void changeCharging(int powerA, int voltage) {
        boolean haveToCharge = powerA >= configService.getMinimumChargingA();
        LOGGER.log(Level.INFO,"haveToCharge=" + haveToCharge+ " currentPowerA="+currentPowerA+" powerA="+powerA+" chargerState="+ storedChargerState);
        if (haveToCharge) {
            if (currentPowerA == null || powerA != currentPowerA.intValue()) {
                metricsLogger.logEVCharging("CHANGEAMPS", powerA);
                charger.changeChargingAmps(powerA, voltage);
                currentPowerA = powerA;
            }
        } else {
            if (currentPowerA != null || storedChargerState.equals(Charger.State.InProgress)) {
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
