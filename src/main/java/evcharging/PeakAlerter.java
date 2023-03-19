package evcharging;

import evcharging.services.ElectricityMeter;
import evcharging.services.NotificationService;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Startup
/**
 *  Check on minute 8 of each quarter of an hour.
 *  Send an alert if the extrapolated usage reaches the configured max peak
 */
public class PeakAlerter {
    public static final Logger LOGGER = Logger.getLogger(PeakAlerter.class.getName());

    @Inject
    ElectricityMeter meter;

    @Inject
    NotificationService notificationService;

    @Inject
    ConfigService configService;

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

}