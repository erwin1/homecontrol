package evcharging;

import evcharging.services.ElectricityMeter;
import evcharging.services.MeterReading;
import evcharging.services.NotificationService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
/**
 *  Check on minute 8 of each quarter of an hour.
 *  Send an alert if the extrapolated usage reaches the configured max peak
 */
public class PeakService {
    public static final Logger LOGGER = Logger.getLogger(PeakService.class.getName());

    MeterReading currentMonth15minUsagePeak;

    @Inject
    ElectricityMeter meter;

    @Inject
    NotificationService notificationService;

    @Inject
    ConfigService configService;

    void onStart(@Observes StartupEvent ev) {
        checkCurrentMonth15minPeak();
    }

    @Scheduled(cron="0 20 0 * * ?")
    public void checkCurrentMonth15minPeak() {
        ZonedDateTime startTime = ZonedDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endTime = ZonedDateTime.now();
        MeterReading highestReading = new MeterReading(startTime, 0);

        while(startTime.isBefore(endTime)) {
            List<MeterReading> readings = meter.getFromGridUsagePer15minBetween(startTime, startTime.plusDays(1));
            for (MeterReading reading : readings) {
                if (reading.getValue() > highestReading.getValue()) {
                    highestReading = reading;
                }
            }
            startTime = startTime.plusDays(1);
        }
        if (highestReading.getValue() != 0) {
            currentMonth15minUsagePeak = highestReading;
        }
        LOGGER.log(Level.INFO, "current month 15min peak = {0} at {1}", new Object[]{highestReading.getValue(), highestReading.getTimestamp()});
    }

    @Scheduled(cron="0 8,23,38,53 * * * ?")
    void run() {
        LOGGER.log(Level.INFO, "Checking peak");
        ZonedDateTime startOfPeriod = ZonedDateTime.now();
        startOfPeriod = startOfPeriod.minusMinutes(startOfPeriod.getMinute() % 15).withSecond(0).withNano(0);
        long readingAtStartOfPeriod = meter.getConsumptionMeterReadingAt(startOfPeriod);
        ZonedDateTime now = ZonedDateTime.now();
        long currentReading = meter.getCurrentConsumptionMeterReading();
        int usageInPeriodWh = (int) (currentReading - readingAtStartOfPeriod);

        int passedTime = (int) (now.toEpochSecond() - startOfPeriod.toEpochSecond());
        int remainingTime = (15 * 60) - passedTime;

        int estimatedUsageInRemainingTime = (int) (usageInPeriodWh * 1.0 / passedTime * remainingTime);

        int totalUsage = estimatedUsageInRemainingTime + usageInPeriodWh;
        LOGGER.log(Level.FINE, "Estimated peak in period: {0}W", totalUsage*4);

        if ((totalUsage*4) > (configService.getMax15minPeak())) {
            String message = MessageFormat.format("Potentially exceeding configured max peak: {0}W", totalUsage * 4);
            LOGGER.log(Level.SEVERE, message);
            notificationService.sendNotification(message);
        }
    }

    public MeterReading getCurrentMonth15minUsagePeak() {
        return currentMonth15minUsagePeak;
    }
}