package homecontrol.services.powercontrol;

import homecontrol.metrics.DetailedMetricsService;
import homecontrol.services.notications.NotificationType;
import homecontrol.services.notications.NotificationService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@ApplicationScoped
public class PowerMonitorService {
    public static final Logger LOGGER = Logger.getLogger(PowerMonitorService.class.getName());

    @Inject
    DetailedMetricsService detailedMetricsService;
    @Inject
    EVControlService evControlService;
    @Inject
    NotificationService notificationService;

    private LocalDateTime lastNotified;


    @Scheduled(every = "1m", concurrentExecution = SKIP)
    public void monitor() {
        BigDecimal exportLast5Mins = detailedMetricsService.getRecentExport(5);
        BigDecimal avgExportW = exportLast5Mins.multiply(BigDecimal.valueOf(12)).multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP);
        LOGGER.info("exportLast5Mins = "+exportLast5Mins+" avgExportW = "+avgExportW);
        if (avgExportW.compareTo(BigDecimal.valueOf(500)) > 0) { //more than 500W export avg in the last 5 min
            if (lastNotified == null || LocalDateTime.now().isAfter(lastNotified.plusMinutes(15))) {
                notificationService.sendNotification(NotificationType.INFO, "PV Export alert: " + avgExportW + "W in last 5 minutes");
                lastNotified = LocalDateTime.now();
            }
        }
    }

}
