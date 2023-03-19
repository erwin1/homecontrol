package evcharging;

import evcharging.impl.sma.SMACharger;
import evcharging.services.EV;
import evcharging.services.EVCharger;
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

    private EVCharger.State chargerState;

    @Inject
    private javax.enterprise.event.Event<EVCharger.State> chargerEvents;




    private boolean inPeakHours() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getDayOfWeek().equals(DayOfWeek.SATURDAY) || now.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return false;
        }
        if (now.getHour() >= 22 || now.getHour() < 6) {
            return false;
        }
        return true;
    }

    @Scheduled(every = "1m", concurrentExecution = SKIP)
    void adaptEVToPowerDifferenceEveryMinute() {
        LOGGER.log(Level.INFO, "Started check in mode {0}", configService.getCurrentMode());

        if (configService.getCurrentMode().equals(Mode.OFF)) {
            return;
        }

        EVCharger.State state = charger.getState();
        if (chargerState != null && !chargerState.equals(state)) {
            chargerEvents.fire(state);
        }
        chargerState = state;

        if (state.equals(EVCharger.State.NotConnected)) {
            LOGGER.log(Level.FINEST, "Charger not connected");
            return;
        }

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

        int powerDifference = powerEstimationService.calculateCurrentPowerDifference(mode);
        ev.requestPowerConsumptionChange(powerDifference, limit);
    }

}
