package homecontrol.metrics;

import homecontrol.services.ev.*;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powercontrol.EVControlService;
import homecontrol.services.powercontrol.PowerPeakService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.MonthlyPowerPeak;
import homecontrol.services.solar.Inverter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
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
    Mailer mailer;

    @ConfigProperty(name = "chargerName")
    String chargerName;
    @ConfigProperty(name = "reportTo")
    String reportTo;

    @Scheduled(cron="0 0 7 * * ?")
    void sendDailySummary() throws IOException {
        LOGGER.log(Level.INFO, "Creating daily summary");
        ZonedDateTime yesterdayStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(1);
        List<CombinedMetrics> metrics = getCombinedMetrics(yesterdayStart, yesterdayStart.plusDays(1).minusSeconds(1), "day");
        CombinedMetrics totals = calculateTotals(metrics);
        metrics.stream().forEach(m -> System.out.println(m.getTimestamp()+" in="+m.getImport()+" ex="+m.getExport()+" ev="+m.getEV()+" pv="+m.getPV()));
        BigDecimal totalUsage = totals.getImport().add(totals.getPV()).subtract(totals.getExport());
        BigDecimal totalUsageWithoutEV = totalUsage.subtract(totals.getEV());
        notificationService.sendNotification("Daily summary for "+yesterdayStart.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))+"\nin="+totals.getImport()+"\nex="+totals.getExport()+"\nev="+totals.getEV()+"\npv="+totals.getPV()+"\ntotal usage = "+totalUsage+"\nwithout ev = "+totalUsageWithoutEV+"\ngrid in without ev="+(totals.getImport().subtract(totals.getEV())));
    }

    @Scheduled(cron="0 5 7 1 * ?")
    void sendMonthlySummary() throws IOException {
        LOGGER.log(Level.INFO, "Creating monthly summary");
        ZonedDateTime lastMonthStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).withDayOfMonth(1).minusMonths(1);
        List<CombinedMetrics> metrics = getCombinedMetrics(lastMonthStart, lastMonthStart.plusMonths(1).minusSeconds(1), "month");
        CombinedMetrics totals = calculateTotals(metrics);
        metrics.stream().forEach(m -> System.out.println(m.getTimestamp()+" in="+m.getImport()+" ex="+m.getExport()+" ev="+m.getEV()+" pv="+m.getPV()));
        BigDecimal totalUsage = totals.getImport().add(totals.getPV()).subtract(totals.getExport());
        BigDecimal totalUsageWithoutEV = totalUsage.subtract(totals.getEV());
        notificationService.sendNotification("Monthly summary for "+lastMonthStart.format(DateTimeFormatter.ofPattern("MMM yy"))+"\nin="+totals.getImport()+"\nex="+totals.getExport()+"\nev="+totals.getEV()+"\npv="+totals.getPV()+"\ntotal usage = "+totalUsage+"\nwithout ev = "+totalUsageWithoutEV+"\ngrid in without ev="+(totals.getImport().subtract(totals.getEV())));
    }

    @Scheduled(cron="0 5 12 1 * ?")
    public void sendEVMonthly() throws IOException {
        LOGGER.log(Level.INFO, "Creating monthly EV summary");
        ZonedDateTime lastMonthStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).withDayOfMonth(1).minusMonths(1);
        List<Metrics> metrics = getMetrics("charger", lastMonthStart, lastMonthStart.plusMonths(1).minusSeconds(1), null, null);
        List<String> labels = metrics.stream()
                .filter(m -> m.getValuet1().compareTo(BigDecimal.ZERO) > 0 || m.getValuet2().compareTo(BigDecimal.ZERO) > 0)
                .filter(m -> m.getLabel() != null)
                .map(m -> m.getLabel())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        StringBuilder text = new StringBuilder("EV charging usage ");
        text.append(lastMonthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"))).append("\n");
        text.append("Charger: ").append(chargerName).append("\n");
        text.append("\n");
        for (String label : labels) {
            text.append("===============================\n");
            text.append("Vehicle label: ").append(label).append("\n\n");
            BigDecimal total = BigDecimal.ZERO;
            metrics = getMetrics("charger", lastMonthStart, lastMonthStart.plusMonths(1).minusSeconds(1), "day", label);
            for(Metrics m : metrics) {
                BigDecimal day = m.getValuet1().add(m.getValuet2());
                total = total.add(day);
                text.append(m.getTimestamp().format((DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                text.append("\t").append(day).append(" kWh\n");
            }
            text.append("\nTOTAL ").append(label).append(":\t\t").append(total).append(" kWh").append("\n\n");
        }
        mailer.send(
            Mail.withText(
                reportTo,
                "EV Charging overview "+chargerName+" "+lastMonthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                text.toString()
            )
        );
    }

    public LiveMetrics getLiveMetrics() {
        var tuple = Uni.combine().all().unis(
                inverter.getCurrentYield(),
                powerMeter.getActivePower(),
                charger.getActivePower()
        ).asTuple().await().atMost(Duration.ofSeconds(10));
        MonthlyPowerPeak monthlyPowerPeak = powerMeter.getMonthlyPowerPeak();
        int currentYield = tuple.getItem1();
        ActivePower activePower = tuple.getItem2();
        int chargerW = tuple.getItem3();


        long gridInOutW = activePower.getActivePower();

        LiveMetrics metrics = new LiveMetrics();
        metrics.setTimestamp(ZonedDateTime.now());
        metrics.setPeakEstimateW(new BigDecimal(powerPeakService.estimatePeakInCurrentPeriod(activePower, 0)));

        if (gridInOutW > 0) {
            metrics.setImportW(new BigDecimal(gridInOutW));
        } else {
            metrics.setExportW(new BigDecimal(gridInOutW).negate());
        }
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
        CombinedMetrics totals = new CombinedMetrics();
        totals.setImportt1(new BigDecimal(sumimt1).divide(new BigDecimal(1000)));
        totals.setImportt2(new BigDecimal(sumimt2).divide(new BigDecimal(1000)));
        totals.setExportt1(new BigDecimal(sumext1).divide(new BigDecimal(1000)));
        totals.setExportt2(new BigDecimal(sumext2).divide(new BigDecimal(1000)));
        totals.setEvt1(new BigDecimal(sumevt1).divide(new BigDecimal(1000)));
        totals.setEvt2(new BigDecimal(sumevt2).divide(new BigDecimal(1000)));
        totals.setPvt1(new BigDecimal(sumpvt1).divide(new BigDecimal(1000)));
        totals.setPvt2(new BigDecimal(sumpvt2).divide(new BigDecimal(1000)));
        return totals;
    }

    private boolean gte(ZonedDateTime t1, ZonedDateTime t2) {
        return t1.toEpochSecond() == t2.toEpochSecond() || t1.isAfter(t2);
    }

    private boolean lte(ZonedDateTime t1, ZonedDateTime t2) {
        return t1.toEpochSecond() == t2.toEpochSecond() || t1.isBefore(t2);
    }

}
