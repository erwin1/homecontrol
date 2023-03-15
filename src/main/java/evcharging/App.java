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

    private boolean stopCheckUntilReconnected = false;

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

    @Scheduled(every = "1m")
    void adaptEVToPowerDifferenceEveryMinute() {
        LOGGER.log(Level.INFO, "Started check in mode {0}", configService.getCurrentMode());

        if (configService.getCurrentMode().equals(Mode.OFF)) {
            return;
        }

        if (charger.getState().equals(EVCharger.State.NotConnected)) {
            LOGGER.log(Level.FINEST, "Charger not connected");
            stopCheckUntilReconnected = false;
            return;
        }

        if (stopCheckUntilReconnected) {
            LOGGER.log(Level.FINE, "Charging was complete. Check stopped until reconnected.");
            return;
        }

        if (ev.isChargingComplete()) {
            stopCheckUntilReconnected = true;
            LOGGER.log(Level.FINE, "Charging completed.");
            return;
        }

        Mode mode = configService.getCurrentMode();
        if (inPeakHours() && mode.equals(Mode.OPTIMAL)) {
            LOGGER.log(Level.INFO, "Switching mode to PV_ONLY because current time is peak hours");
            mode = Mode.PV_ONLY;
        }
        int powerDifference = powerEstimationService.calculateCurrentPowerDifference(mode);
        ev.requestPowerConsumptionChange(powerDifference);
    }

}
