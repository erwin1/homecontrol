package homecontrol.metrics;

import homecontrol.services.config.ConfigService;
import homecontrol.services.ev.*;
import homecontrol.services.notications.NotificationType;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powercontrol.EVControlService;
import homecontrol.services.powercontrol.PowerPeakService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.HomeBatteryPowerMeter;
import homecontrol.services.powermeter.MonthlyPowerPeak;
import homecontrol.services.solar.Inverter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import org.openpdf.text.DocumentException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetricsService {
    public static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    private MetricsLogger metricsLogger;

    @Inject
    private ElectricalPowerMeter powerMeter;

    @Inject
    private HomeBatteryPowerMeter homeBatteryPowerMeter;

    @Inject
    Inverter inverter;

    @Inject
    Charger charger;

    @Inject
    PowerPeakService powerPeakService;

    @Inject
    EVControlService evControlService;

    @Inject
    NotificationService notificationService;

    @Inject
    ConfigService configService;

    @Inject
    Mailer mailer;

    @Inject
    @Any
    Instance<ElectricVehicle> allVehicles;

    @Inject
    ReportConfig reportConfig;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "chargerName")
    String chargerName;

    @Scheduled(cron="0 0 7 * * ?")
    public void sendDailySummary() throws IOException {
        LOGGER.log(Level.INFO, "Creating daily summary");
        ZonedDateTime yesterdayStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(1);
        sendSummary("*Daily summary "+yesterdayStart.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "*", yesterdayStart, yesterdayStart.plusDays(1).minusSeconds(1));
    }

    private void sendSummary(String label, ZonedDateTime start, ZonedDateTime end) throws IOException {
        List<CombinedMetrics> metrics = getCombinedMetrics(start, end, "day");
        CombinedMetrics totals = calculateTotals(metrics);
        metrics.stream().forEach(m -> System.out.println(m.getTimestamp()+" in="+m.getImport()+" ex="+m.getExport()+" ev="+m.getEV()+" pv="+m.getPV()));
        BigDecimal totalUsage = totals.getTotalUsage();
        BigDecimal totalUsageWithoutEV = totals.getTotalUsageWithoutEV();
        BigDecimal gridImportHome = totals.getImport().subtract(totals.getEV());
        StringBuilder msg = new StringBuilder();
        msg.append(label).append("\n");
        msg.append("📊 ").append(r1(totalUsage)).append(" kWh    ☀️ ").append(r1(totalUsage.subtract(totals.getImport()))).append(" kWh   \uD83D\uDD0B ").append(r1(totals.getHomeBatteryExport())).append(" kWh    ⬇️ ").append(r1(totals.getImport())).append(" kWh\n");
        msg.append("🏠 ").append(r1(totalUsageWithoutEV)).append(" kWh    ☀️ ").append(r1((totals.getPV().subtract(gridImportHome)).min(totalUsageWithoutEV))).append(" kWh    ⬇️ ").append(r1(gridImportHome)).append(" kWh\n");
        Map<String,BigDecimal> evDetails = getEVTotalsGroupedByLabel(start, end);
        if (!evDetails.isEmpty()) {
            BigDecimal totalin = totals.getImport();
            for (String vehicle : evDetails.keySet()) {
                BigDecimal usage = evDetails.get(vehicle);
                if (usage.compareTo(new BigDecimal("0.5")) > 0) {
                    BigDecimal gridForVehicle;
                    if (totalin.compareTo(usage) > 0) {
                        gridForVehicle = usage;
                        totalin = totalin.subtract(usage);
                    } else {
                        gridForVehicle = totalin;
                        totalin = BigDecimal.ZERO;
                    }
                    msg.append("🚗 ").append(r1(usage)).append(" kWh    ☀️ ").append(r1(usage.subtract(gridForVehicle))).append(" kWh    ⬇️ ").append(r1(gridForVehicle)).append(" kWh     ").append(vehicle).append("\n");
                }
            }
        }
        msg.append("\n");
        msg.append("☀️ ").append(r1(totals.getPV())).append(" kWh\n");
        msg.append("\uD83D\uDD0B ").append(r1(totals.getHomeBatteryImport())).append(" kWh\n");
        msg.append("⬆️ ").append(r1(totals.getExport())).append(" kWh\n");

        notificationService.sendNotification(NotificationType.IMPORTANT, msg.toString());
    }

    @Scheduled(cron="0 5 7 1 * ?")
    public void sendMonthlySummary() throws IOException {
        LOGGER.log(Level.INFO, "Creating monthly summary");
        ZonedDateTime lastMonthStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).withDayOfMonth(1).minusMonths(1);
        sendSummary("*Monthly summary for "+lastMonthStart.format(DateTimeFormatter.ofPattern("MMM yy"))+"*", lastMonthStart, lastMonthStart.plusMonths(1).minusSeconds(1));
    }

    @Scheduled(cron="0 5 7 1 * ?")
    public void sendEVMonthly() throws IOException {
        LOGGER.log(Level.INFO, "Creating monthly EV summary");
        ZonedDateTime lastMonthStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).withDayOfMonth(1).minusMonths(1);
        String monthLabel = lastMonthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"));

        for (ReportConfig.VehicleReportConfig vehicleConfig : reportConfig.vehicles()) {
            List<Metrics> dailyMetrics = getMetrics("charger", lastMonthStart, lastMonthStart.plusMonths(1).minusSeconds(1), "day", vehicleConfig.label());
            boolean hasData = dailyMetrics.stream().anyMatch(m -> m.getValuet1().add(m.getValuet2()).compareTo(BigDecimal.ZERO) > 0);
            if (!hasData) {
                LOGGER.info("No charging data for " + vehicleConfig.label() + " in " + monthLabel + ", skipping report");
                continue;
            }

            Optional<EVState> odo = Optional.empty();
            for (ElectricVehicle ev : allVehicles) {
                if (ev.isConfigured() && ev.getName().equals(vehicleConfig.label())) {
                    try {
                        odo = ev.getOdoMeter();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to get ODO for " + vehicleConfig.label(), e);
                    }
                    break;
                }
            }

            Optional<BigDecimal> tariff = configService.getTariffFor(lastMonthStart.toLocalDate());
            Mail mail = Mail.withText(
                vehicleConfig.to(),
                "EV Charging report " + chargerName + " / " + vehicleConfig.label() + " " + monthLabel,
                "Please find attached the EV charging report for " + vehicleConfig.label() + " – " + monthLabel + "."
            );
            try {
                byte[] pdf = EVReportPdfGenerator.generate(
                    chargerName,
                    vehicleConfig.label(),
                    vehicleConfig.vin(),
                    odo,
                    lastMonthStart,
                    dailyMetrics,
                    tariff
                );
                mail.addAttachment("EV-report-" + lastMonthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf", pdf, "application/pdf");
            } catch (DocumentException e) {
                LOGGER.log(Level.WARNING, "Failed to generate PDF report for " + vehicleConfig.label(), e);
            }
            mailer.send(mail);
        }
    }

    public LiveMetrics getLiveMetrics() {
        var tuple = Uni.combine().all().unis(
                inverter.getCurrentYield(),
                powerMeter.getPower(),
                homeBatteryPowerMeter.getActivePower(),
                homeBatteryPowerMeter.getCurrentReading(),
                charger.getActivePowerW()
        ).asTuple().await().atMost(Duration.ofSeconds(10));
        MonthlyPowerPeak monthlyPowerPeak = tuple.getItem2().getMonthlyPowerPeak();
        int currentYield = tuple.getItem1();
        ActivePower activePower = tuple.getItem2().getActivePower();
        int chargerW = tuple.getItem5();


        long gridInOutW = activePower.getActivePower();

        LiveMetrics metrics = new LiveMetrics();
        metrics.setTimestamp(ZonedDateTime.now());
        metrics.setPeakEstimateW(new BigDecimal(powerPeakService.estimatePeakInCurrentPeriod(activePower, 0)));
        metrics.setEvAmps(charger.getCurrentChargingAmps());

        if (gridInOutW > 0) {
            metrics.setImportW(new BigDecimal(gridInOutW));
        } else {
            metrics.setExportW(new BigDecimal(gridInOutW).negate());
        }

        metrics.setHomeBatteryPowerW(new BigDecimal(tuple.getItem3().getActivePower()));
        metrics.setHomeBatteryStateOfCharge(tuple.getItem4().getState_of_charge_pct());

        metrics.setEvW(new BigDecimal(chargerW));
        metrics.setPvW(new BigDecimal(currentYield));
        metrics.setEvConnected(!charger.getCurrentState(StateRefresh.CACHED).equals(Charger.State.NotConnected));
        if (metrics.isEvConnected()) {
            ElectricVehicle connectedEv = evControlService.getConnectedVehicle();
            metrics.setEvConnectedName(connectedEv != null ? connectedEv.getName() : "Unknown");
            try {
                EVState evState = evControlService.getCurrentState(StateRefresh.CACHED_OR_NULL);
                metrics.setEvBatteryLevel(evState != null ? evState.getBattery_level() : 0);
            } catch (EVException e) {
                throw new RuntimeException(e);
            }
        }
        metrics.setMonthlyPowerPeakW(new BigDecimal(monthlyPowerPeak.getValue()));
        metrics.setMonthlyPowerPeakTimestamp(monthlyPowerPeak.getTimestamp());
        metrics.setImportAverageW(new BigDecimal(activePower.getActivePowerAverage()));
        return metrics;
    }

    public List<CombinedMetrics> getCombinedMetrics(ZonedDateTime start, ZonedDateTime end, String period) throws IOException {
        Map<ZonedDateTime, CombinedMetrics> combined = new HashMap<>();
        List<Metrics> list = getMetrics("gridImport", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
               if (v == null) {
                   v = new CombinedMetrics();
                   v.setTimestamp(m.getTimestamp());
               }
               v.setImportt1(m.getValuet1());
               v.setImportt2(m.getValuet2());
               return v;
            });
        }
        list = getMetrics("gridExport", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new CombinedMetrics();
                    v.setTimestamp(m.getTimestamp());
                }
                v.setExportt1(m.getValuet1());
                v.setExportt2(m.getValuet2());
                return v;
            });
        }
        list = getMetrics("charger", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new CombinedMetrics();
                    v.setTimestamp(m.getTimestamp());
                }
                v.setEvt1(m.getValuet1());
                v.setEvt2(m.getValuet2());
                return v;
            });
        }
        list = getMetrics("pv", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new CombinedMetrics();
                    v.setTimestamp(m.getTimestamp());
                }
                v.setPvt1(m.getValuet1());
                v.setPvt2(m.getValuet2());
                return v;
            });
        }
        list = getMetrics("homeBatteryImport", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new CombinedMetrics();
                    v.setTimestamp(m.getTimestamp());
                }
                System.out.println("IMPORT "+m.getValuet1()+" "+m.getValuet2());
                v.setHomeBatteryImportt1(m.getValuet1());
                v.setHomeBatteryImportt2(m.getValuet2());
                return v;
            });
        }
        list = getMetrics("homeBatteryExport", start, end, period, null);
        for(Metrics m : list) {
            combined.compute(m.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new CombinedMetrics();
                    v.setTimestamp(m.getTimestamp());
                }
                System.out.println("EXPORt "+m.getValuet1()+" "+m.getValuet2());
                v.setHomeBatteryExportt1(m.getValuet1());
                v.setHomeBatteryExportt2(m.getValuet2());
                return v;
            });
        }
        return combined.values().stream().sorted(Comparator.comparing(CombinedMetrics::getTimestamp)).collect(Collectors.toList());
    }

    public Map<String,BigDecimal> getEVTotalsGroupedByLabel(ZonedDateTime start, ZonedDateTime end) throws IOException {
        List<Metrics> metrics = getMetrics("charger", start, end, null, null);
        List<String> labels = metrics.stream()
                .filter(m -> m.getValuet1().compareTo(BigDecimal.ZERO) > 0 || m.getValuet2().compareTo(BigDecimal.ZERO) > 0)
                .filter(m -> m.getLabel() != null)
                .map(m -> m.getLabel())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Map<String, BigDecimal> result = new HashMap<>();
        for (String label : labels) {
            BigDecimal total = BigDecimal.ZERO;
            for(Metrics m : metrics) {
                if (label.equals(m.getLabel())) {
                    total = total.add(m.getValuet1()).add(m.getValuet2());
                }
            }
            result.put(label, total);
        }
        return result;
    }

    public List<Metrics> getMetrics(String type, ZonedDateTime start, ZonedDateTime end, String period, String label) throws IOException {
        List<Metrics> list = null;
        if (type.equals("gridImport")) {
            list = metricsLogger.getGridImport();
        } else if (type.equals("charger")) {
            list = metricsLogger.getCharger();
        } else if (type.equals("gridExport")) {
            list = metricsLogger.getGridExport();
        } else if (type.equals("pv")) {
            list = metricsLogger.getPv();
        } else if (type.equals("homeBatteryImport")) {
            list = metricsLogger.getHomeBatteryImport();
        } else if (type.equals("homeBatteryExport")) {
            list = metricsLogger.getHomeBatteryExport();
        }

        if (period == null) {
            return list.stream()
                    .filter(m -> label == null || label.equals(m.getLabel()))
                    .filter(m -> (gte(m.getTimestamp(), start) && lte(m.getTimestamp(), end)))
                    .sorted(Comparator.comparing(Metrics::getTimestamp))
                    .collect(Collectors.toList());
        } else if (period.equals("hour")) {
            return filterAndGroupBy("hour", list, start, end, label);
        } else if (period.equals("day")) {
            return filterAndGroupBy("day", list, start, end, label);
        } else if (period.equals("month")) {
            return filterAndGroupBy("month", list, start, end, label);
        }
        return null;
    }

    private ZonedDateTime truncate(String type, ZonedDateTime t) {
        if (type.equals("hour")) {
            t = t.truncatedTo(ChronoUnit.HOURS);
        } else if (type.equals("day")) {
            t = t.truncatedTo(ChronoUnit.DAYS);
        } else if (type.equals("month")) {
            t = t.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        }
        return t;
    }

    public List<Metrics> filterAndGroupBy(String groupBy, List<Metrics> list, ZonedDateTime start, ZonedDateTime end, String label) {
        final ZonedDateTime startTime = start.withHour(0).withMinute(0).withSecond(0).withNano(0);
        Map<ZonedDateTime, Integer> mapt1 = list.stream()
                .map(m -> {
                    if (label == null || label.equals(m.getLabel())) {
                        return m;
                    } else {
                        return new Metrics(m.getTimestamp(), m.getPeriod(), BigDecimal.ZERO, BigDecimal.ZERO);
                    }
                })
                .filter(m -> (gte(m.getTimestamp(), startTime) && lte(m.getTimestamp(), end)))
                .collect(Collectors.groupingBy((e) -> truncate(groupBy, e.getTimestamp()), Collectors.summingInt(m -> m.getValuet1().multiply(new BigDecimal("1000")).intValue())));
        Map<ZonedDateTime, Integer> mapt2 = list.stream()
                .map(m -> {
                    if (label == null || label.equals(m.getLabel())) {
                        return m;
                    } else {
                        return new Metrics(m.getTimestamp(), m.getPeriod(), BigDecimal.ZERO, BigDecimal.ZERO);
                    }
                })
                .filter(m -> (gte(m.getTimestamp(), startTime) && lte(m.getTimestamp(), end)))
                .collect(Collectors.groupingBy((e) -> truncate(groupBy, e.getTimestamp()), Collectors.summingInt(m -> m.getValuet2().multiply(new BigDecimal("1000")).intValue())));
        Map<ZonedDateTime, Metrics> map = new HashMap<>();

        for(ZonedDateTime d : mapt1.keySet()) {
            map.compute(d, (x,y) -> {
                if (y == null) {
                    y = new Metrics(d, groupBy, BigDecimal.ZERO, BigDecimal.ZERO);
                }
                y.setValuet1(y.getValuet1().add(new BigDecimal(mapt1.get(d)).divide(new BigDecimal(1000))));
                return y;
            });
        }
        for(ZonedDateTime d : mapt2.keySet()) {
            map.compute(d, (x,y) -> {
                if (y == null) {
                    y = new Metrics(d, groupBy, BigDecimal.ZERO, BigDecimal.ZERO);
                }
                y.setValuet2(y.getValuet2().add(new BigDecimal(mapt2.get(d)).divide(new BigDecimal(1000))));
                return y;
            });
        }
        return map.values().stream().sorted(Comparator.comparing(Metrics::getTimestamp)).collect(Collectors.toList());
    }

    public CombinedMetrics calculateTotals(List<CombinedMetrics> importMetrics) {
        int sumimt1 = importMetrics.stream().filter(m -> m.getImportt1() != null).mapToInt(m -> m.getImportt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumimt2 = importMetrics.stream().filter(m -> m.getImportt2() != null).mapToInt(m -> m.getImportt2().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumext1 = importMetrics.stream().filter(m -> m.getExportt1() != null).mapToInt(m -> m.getExportt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumext2 = importMetrics.stream().filter(m -> m.getExportt2() != null).mapToInt(m -> m.getExportt2().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumevt1 = importMetrics.stream().filter(m -> m.getEvt1() != null).mapToInt(m -> m.getEvt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumevt2 = importMetrics.stream().filter(m -> m.getEvt2() != null).mapToInt(m -> m.getEvt2().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumpvt1 = importMetrics.stream().filter(m -> m.getPvt1() != null).mapToInt(m -> m.getPvt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumpvt2 = importMetrics.stream().filter(m -> m.getPvt2() != null).mapToInt(m -> m.getPvt2().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumbatimt1 = importMetrics.stream().filter(m -> m.getHomeBatteryImportt1() != null).mapToInt(m -> m.getHomeBatteryImportt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumbatimt2 = importMetrics.stream().filter(m -> m.getHomeBatteryImportt2() != null).mapToInt(m -> m.getHomeBatteryImportt2().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumbatext1 = importMetrics.stream().filter(m -> m.getHomeBatteryExportt1() != null).mapToInt(m -> m.getHomeBatteryExportt1().multiply(new BigDecimal(1000)).intValue()).sum();
        int sumbatext2 = importMetrics.stream().filter(m -> m.getHomeBatteryExportt2() != null).mapToInt(m -> m.getHomeBatteryExportt2().multiply(new BigDecimal(1000)).intValue()).sum();
        CombinedMetrics totals = new CombinedMetrics();
        totals.setImportt1(new BigDecimal(sumimt1).divide(new BigDecimal(1000)));
        totals.setImportt2(new BigDecimal(sumimt2).divide(new BigDecimal(1000)));
        totals.setExportt1(new BigDecimal(sumext1).divide(new BigDecimal(1000)));
        totals.setExportt2(new BigDecimal(sumext2).divide(new BigDecimal(1000)));
        totals.setEvt1(new BigDecimal(sumevt1).divide(new BigDecimal(1000)));
        totals.setEvt2(new BigDecimal(sumevt2).divide(new BigDecimal(1000)));
        totals.setPvt1(new BigDecimal(sumpvt1).divide(new BigDecimal(1000)));
        totals.setPvt2(new BigDecimal(sumpvt2).divide(new BigDecimal(1000)));
        totals.setHomeBatteryImportt1(new BigDecimal(sumbatimt1).divide(new BigDecimal(1000)));
        totals.setHomeBatteryImportt2(new BigDecimal(sumbatimt2).divide(new BigDecimal(1000)));
        totals.setHomeBatteryExportt1(new BigDecimal(sumbatext1).divide(new BigDecimal(1000)));
        totals.setHomeBatteryExportt2(new BigDecimal(sumbatext2).divide(new BigDecimal(1000)));
        return totals;
    }

    private static BigDecimal r1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }

    private boolean gte(ZonedDateTime t1, ZonedDateTime t2) {
        return t1.toEpochSecond() == t2.toEpochSecond() || t1.isAfter(t2);
    }

    private boolean lte(ZonedDateTime t1, ZonedDateTime t2) {
        return t1.toEpochSecond() == t2.toEpochSecond() || t1.isBefore(t2);
    }

}
