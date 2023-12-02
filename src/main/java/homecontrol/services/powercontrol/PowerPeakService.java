package homecontrol.services.powercontrol;

import homecontrol.services.config.ConfigService;
import homecontrol.services.config.PeakStrategy;
import homecontrol.services.ev.Charger;
import homecontrol.services.ev.StateRefresh;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.MonthlyPowerPeak;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.tuples.Tuple3;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PowerPeakService {
    public static final Logger LOGGER = Logger.getLogger(PowerPeakService.class.getName());

    @Inject
    private ConfigService configService;
    @Inject
    private ElectricalPowerMeter electricalPowerMeter;
    @Inject
    private Charger charger;
    @Inject
    private NotificationService notificationService;

    @Inject
    private Clock clock;

    private ZonedDateTime lastAlertTime;
    private MonthlyPowerPeak monthlyPowerPeak;

    public int getCurrentMonth15minPeak() {
        if (configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_LIMITED)
                || configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_UNLIMITED)) {
            try {
                MonthlyPowerPeak currentMonth15minUsagePeak = electricalPowerMeter.getMonthlyPowerPeak();
                if (currentMonth15minUsagePeak != null) {
                    ZonedDateTime now = ZonedDateTime.now(clock);
                    if (now.getMonth() == currentMonth15minUsagePeak.getTimestamp().getMonth()) {
                        int currentPeak = Math.max(currentMonth15minUsagePeak.getValue(), configService.getMin15minPeak());
                        if (configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_LIMITED)) {
                            return Math.min(currentPeak, configService.getMax15minPeak());
                        } else {
                            return currentPeak;
                        }
                    }
                }
            }catch (Exception e) {
                LOGGER.log(Level.SEVERE, "could not get current month peak. defaulting to minimum peak", e);
            }
            return configService.getMin15minPeak();
        } else {
            return configService.getMax15minPeak();
        }
    }


    @Scheduled(cron="0 2-14,17-29,32-44,47-59 * * * ?")
    void checkPeak() {
        LOGGER.log(Level.INFO, "Checking peak");
        MonthlyPowerPeak currentMonthPeak = electricalPowerMeter.getMonthlyPowerPeak();

        if (this.monthlyPowerPeak != null) {
            if (!this.monthlyPowerPeak.getTimestamp().equals(currentMonthPeak.getTimestamp())) {
                notificationService.sendNotification("!!! New Peak: "+currentMonthPeak.getValue()+"W\n"+currentMonthPeak.getTimestamp());
            }
        }
        this.monthlyPowerPeak = currentMonthPeak;

        int currentPeakW = Math.max(currentMonthPeak.getValue(), configService.getMin15minPeak());
        var tuple = estimatePeakInCurrentPeriod();
        int estimatedPeakW = tuple.getItem1();

        if (estimatedPeakW > currentPeakW) {
            ZonedDateTime now = ZonedDateTime.now(clock);
            if (lastAlertTime == null || !isSame15minPeriod(now, lastAlertTime)) {
                this.lastAlertTime = ZonedDateTime.now(clock);
                String message = MessageFormat.format(
                        "-= PEAK WARNING =-\n" +
                                "Current power:         {0}W\n" +
                                "Current period peak:   {1}W\n" +
                                "Charging:              {2}W\n" +
                                "Current month peak:    {3}W\n" +
                                "Peak date:             {4}\n" +
                                "Estimated new peak:    {5}W\n",
                        tuple.getItem2().getActivePower(),
                        tuple.getItem2().getActivePowerAverage(),
                        tuple.getItem3(),
                        currentMonthPeak.getValue(),
                        currentMonthPeak.getTimestamp(),
                        estimatedPeakW);
                LOGGER.log(Level.SEVERE, message);
                notificationService.sendNotification(message);
            }
        }
    }

    private boolean isSame15minPeriod(ZonedDateTime now, ZonedDateTime other) {
        if (now.truncatedTo(ChronoUnit.DAYS).equals(other.truncatedTo(ChronoUnit.DAYS))) {
            int x1 = (now.getHour() * 60 + now.getMinute()) / 15;
            int x2 = (other.getHour() * 60 + other.getMinute()) / 15;
            return x1 == x2;
        }
        return false;
    }

    Tuple3<Integer,ActivePower,Integer> estimatePeakInCurrentPeriod() {
        ActivePower activePower = electricalPowerMeter.getActivePower().await().atMost(Duration.ofSeconds(10));
        int chargingW = 0;
        if (charger.getCurrentState(StateRefresh.CACHED).equals(Charger.State.InProgress)) {
            chargingW = charger.getActivePower().await().atMost(Duration.ofSeconds(10));
        }
        return Tuple3.of(estimatePeakInCurrentPeriod(activePower, chargingW), activePower, chargingW);
    }

    public int estimatePeakInCurrentPeriod(ActivePower activePower, int chargingW) {
        LOGGER.log(Level.INFO, "Estimate peak");

        ZonedDateTime now = activePower.getTimestamp();
        ZonedDateTime startOfPeriod = now.minusMinutes(now.getMinute() % 15).withSecond(0).withNano(0);
        int passedTime = (int) (now.toEpochSecond() - startOfPeriod.toEpochSecond());
        int remainingTime = (15 * 60) - passedTime;
        if (passedTime == 0) {
            return activePower.getActivePower() - chargingW;
        }
        if (remainingTime == 0) {
            return activePower.getActivePowerAverage();
        }

        int usageInPeriodWh = activePower.getActivePowerAverage() / 4;

        int estimatedTotalUsageInRemainingTimeWh = (int) (usageInPeriodWh * 1.0 / passedTime * remainingTime);

        int estimatedChargingUsageInRemainingTimeWh = (int)(chargingW / 4.0 / (15*60) * remainingTime);

        int totalEstmatedUsageInPeriodWh = estimatedTotalUsageInRemainingTimeWh + usageInPeriodWh - estimatedChargingUsageInRemainingTimeWh;

        int estimatedPeakInPeriodW = totalEstmatedUsageInPeriodWh * 4;

        LOGGER.log(Level.INFO, "Estimated peak in period: {0}W", estimatedPeakInPeriodW);

        return estimatedPeakInPeriodW;
    }

}
