package evcharging;

import evcharging.impl.sma.SMACharger;
import evcharging.services.EV;
import evcharging.services.EVCharger;
import evcharging.services.NotificationService;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@ApplicationScoped
@Startup
public class App {
    public static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Inject
    SMACharger charger;

    @Inject
    ConfigService configService;

    @Inject
    PowerEstimationService powerEstimationService;

    @Inject
    EV ev;

    @Inject
    NotificationService notificationService;

    private EVCharger.State chargerState;

    @Inject
    javax.enterprise.event.Event<EVCharger.State> chargerEvents;

    private boolean inPeakHours() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getDayOfWeek().equals(DayOfWeek.SATURDAY) || now.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return false;
        }
        if (now.getHour() >= 22 || now.getHour() < 7) {
            return false;
        }
        return true;
    }

    @Scheduled(every = "1m", concurrentExecution = SKIP)
    void adaptEVToPowerDifferenceEveryMinute() {
        LOGGER.log(Level.INFO, "Started check in mode {0}", configService.getCurrentMode());

        if (configService.getCurrentMode().equals(Mode.OFF)) {
            //stop running if mode = OFF
            return;
        }

        EVCharger.State state = charger.getState();
        fireEVChargerStateEventIfNeeded(state);

        if (state.equals(EVCharger.State.NotConnected)) {
            //stop running if charger is not connected
            LOGGER.log(Level.FINEST, "Charger not connected");
            return;
        }

        //define mode based on configuration and current time:
        Mode mode = configService.getCurrentMode();
        Integer limit = null;//use default limit
        if (mode.equals(Mode.OPTIMAL)) {
            if (inPeakHours()) {
                LOGGER.log(Level.INFO, "Switching mode to PV_ONLY because current time is peak hours");
                mode = Mode.PV_ONLY;
            } else {
                if (ev.getCurrentBatteryLevel() >= configService.getChargeLimitFromGrid()) {
                    LOGGER.log(Level.INFO, "Switching mode to PV_ONLY because from grid limit was reached: "+ev.getCurrentBatteryLevel());
                    mode = Mode.PV_ONLY;
                } else {
                    //use custom from grid limit
                    limit = configService.getChargeLimitFromGrid();
                }
            }
        }

        try {
            int powerDifference = powerEstimationService.calculateCurrentPowerDifference(mode);
            ev.requestPowerConsumptionChange(powerDifference, limit);
        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error in EVCharging", e);
            notificationService.sendNotification("Error in EV charging "+e);
        }
    }

    private void fireEVChargerStateEventIfNeeded(EVCharger.State state) {
        if (this.chargerState != null && !this.chargerState.equals(state)) {
            this.chargerEvents.fire(state);
        }
        this.chargerState = state;
    }

}
