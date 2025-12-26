package homecontrol.metrics;

import homecontrol.services.ev.Charger;
import homecontrol.services.ev.ElectricVehicle;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powercontrol.EVControlService;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.MeterReading;
import homecontrol.services.solar.Inverter;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetricsLogger {
    public static final Logger LOGGER = Logger.getLogger(MetricsLogger.class.getName());

    @ConfigProperty(name = "METRICSLOGGER_DATA")
    Optional<String> dataPath;

    @ConfigProperty(name = "METRICSLOGGER_LABEL_IF_UNKNOWN")
    String labelIfUnknown;

    @Inject
    ElectricalPowerMeter powerMeter;

    @Inject
    private EVControlService evControlService;

    @Inject
    Charger evCharger;
    @Inject
    Inverter inverter;

    @Inject
    NotificationService notificationService;

    @PostConstruct
    void readFiles() throws IOException {
        LOGGER.info("initializing metrics logger");
        gridExport = readFiles(Path.of(dataPath.get()).resolve("total_power_export_t1_kwh.csv"), Path.of(dataPath.get()).resolve("total_power_export_t2_kwh.csv"));
        gridImport = readFiles(Path.of(dataPath.get()).resolve("total_power_import_t1_kwh.csv"), Path.of(dataPath.get()).resolve("total_power_import_t2_kwh.csv"));
        charger = readFile(Path.of(dataPath.get()).resolve("total_power_charger_kwh.csv"));
        pv = readFile(Path.of(dataPath.get()).resolve("total_power_pv_kwh.csv"));
        LOGGER.info("initializing metrics logger done");
    }

    List<Metrics> gridImport;
    List<Metrics> gridExport;
    List<Metrics> charger;
    List<Metrics> pv;

    public List<Metrics> getGridImport() {
        return gridImport;
    }

    public List<Metrics> getGridExport() {
        return gridExport;
    }

    public List<Metrics> getCharger() {
        return charger;
    }

    public List<Metrics> getPv() {
        return pv;
    }


    @Scheduled(cron="0 0 * * * ?")
    void log() throws IOException {
        if (!dataPath.isPresent()) {
            LOGGER.severe("no data logger defined. not logging.");
            return;
        }
        LOGGER.log(Level.INFO, "Logging metrics");
        ZonedDateTime ts = ZonedDateTime.now();
        ts = ts.withMinute(0).withSecond(0).withNano(0);

        Path path = Path.of(dataPath.get());
        String tsString = ts.withZoneSameInstant(ZoneId.of("Europe/Brussels")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));

        try {
            Path importt1 = path.resolve("total_power_import_t1_kwh.csv");
            Path importt2 = path.resolve("total_power_import_t2_kwh.csv");
            Path exportt1 = path.resolve("total_power_export_t1_kwh.csv");
            Path exportt2 = path.resolve("total_power_export_t2_kwh.csv");
            Path gas = path.resolve("total_gas_m3.csv");

            MeterReading data = powerMeter.getCurrentReading();
            try (BufferedWriter writer = Files.newBufferedWriter(importt1, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ data.getTotal_power_import_t1_kwh()+"\n");
            }
            try (BufferedWriter writer = Files.newBufferedWriter(importt2, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ data.getTotal_power_import_t2_kwh()+"\n");
            }
            try (BufferedWriter writer = Files.newBufferedWriter(exportt1, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ data.getTotal_power_export_t1_kwh()+"\n");
            }
            try (BufferedWriter writer = Files.newBufferedWriter(exportt2, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ data.getTotal_power_export_t2_kwh()+"\n");
            }

            try (BufferedWriter writer = Files.newBufferedWriter(gas, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ data.getTotal_gas_m3()+"\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
            notificationService.sendNotification("MetricsLogger: Could not log hwep1 data "+e);
        }

        try {
            long pvReading = inverter.getYieldMeterReading();

            Path pv = path.resolve("total_power_pv_kwh.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(pv, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ pvReading+"\n");
            }

        } catch(Exception e) {
            e.printStackTrace();
            notificationService.sendNotification("MetricsLogger: Could not log SMA PV data "+e);
        }

        try {
            long chargerReading = evCharger.getChargingMeterReading();
            String label = "none";
            ElectricVehicle connectedVehicle = evControlService.getConnectedVehicle();
            if (connectedVehicle != null) {
                label = connectedVehicle.getName();
            }
            Path charger = path.resolve("total_power_charger_kwh.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(charger, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ chargerReading+","+label+"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            notificationService.sendNotification("MetricsLogger: Could not log SMA Charger data "+e);
        }

        readFiles();
    }

    List<Metrics> readFiles(Path t1, Path t2) throws IOException {
        List<String> importt1 = Files.readAllLines(t1);
        List<String> importt2 = Files.readAllLines(t2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

        List<Metrics> metricst1 = new LinkedList<>();
        for(String line : importt1) {
            String[] parsed = line.split(",");
            ZonedDateTime time = ZonedDateTime.parse(parsed[0], formatter).withZoneSameInstant(ZoneId.of("Europe/Brussels"));
            BigDecimal value = new BigDecimal(parsed[1]);
            Metrics metrics = new Metrics(time, "hour", value, BigDecimal.ZERO);
            metricst1.add(metrics);
        }
        List<Metrics> metricst2 = new LinkedList<>();
        for(String line : importt2) {
            String[] parsed = line.split(",");
            ZonedDateTime time = ZonedDateTime.parse(parsed[0], formatter).withZoneSameInstant(ZoneId.of("Europe/Brussels"));
            BigDecimal value = new BigDecimal(parsed[1]);
            Metrics metrics = new Metrics(time, "hour", BigDecimal.ZERO, value);
            metricst2.add(metrics);
        }

        Map<ZonedDateTime, Metrics> combined = new HashMap<>();

        for(int i = 0; i < metricst1.size() - 1; i++) {
            Metrics m1 = metricst1.get(i);
            Metrics m2 = metricst1.get(i + 1);
            combined.compute(m1.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new Metrics(m1.getTimestamp(), "hour", BigDecimal.ZERO, BigDecimal.ZERO);
                }
                v.setValuet1(m2.getValuet1().subtract(m1.getValuet1()));
                return v;
            });
        }
        for(int i = 0; i < metricst2.size() - 1; i++) {
            Metrics m1 = metricst2.get(i);
            Metrics m2 = metricst2.get(i + 1);
            combined.compute(m1.getTimestamp(), (k,v) -> {
                if (v == null) {
                    v = new Metrics(m1.getTimestamp(), "hour", BigDecimal.ZERO, BigDecimal.ZERO);
                }
                v.setValuet2(m2.getValuet2().subtract(m1.getValuet2()));
                return v;
            });
        }

        return combined.values().stream().sorted(Comparator.comparing(Metrics::getTimestamp)).collect(Collectors.toList());
    }

    public void logChargerDataPoint(String label) {
        if (!dataPath.isPresent()) {
            LOGGER.severe("no data logger defined. not logging.");
            return;
        }
        LOGGER.log(Level.INFO, "Logging charging data point");

        Path path = Path.of(dataPath.get());
        String tsString = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Brussels")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));

        try {
            long chargerReading = evCharger.getChargingMeterReading();

            Path charger = path.resolve("total_power_charger_kwh.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(charger, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+ chargerReading+","+label+"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            notificationService.sendNotification("MetricsLogger: Could not log extra SMA Charger data point "+e);
        }
    }

    public void logEVCharging(String action, int chargeAmps) {
        if (!dataPath.isPresent()) {
            LOGGER.severe("no data logger defined. not logging.");
            return;
        }
        LOGGER.log(Level.INFO, "Logging charging state change");

        ZonedDateTime ts = ZonedDateTime.now();

        Path path = Path.of(dataPath.get());
        String tsString = ts.withZoneSameInstant(ZoneId.of("Europe/Brussels")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));

        try {
            Path evchargingstates = path.resolve("ev_charging_statechanges.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(evchargingstates, StandardOpenOption.CREATE,StandardOpenOption.APPEND)) {
                writer.write(tsString+","+action+","+chargeAmps+"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            notificationService.sendNotification("MetricsLogger: Could not log EV Charging data "+e);
        }
    }

    List<Metrics> readFile(Path f) throws IOException {
        List<String> charger = Files.readAllLines(f);

        List<Metrics> toCharger = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

        List<Metrics> metricscharger = new LinkedList<>();
        for(String line : charger) {
            String[] parsed = line.split(",");
            String label = parsed.length > 2 ? parsed[2] : labelIfUnknown;
            ZonedDateTime time = ZonedDateTime.parse(parsed[0], formatter).withZoneSameInstant(ZoneId.of("Europe/Brussels"));
            BigDecimal value = new BigDecimal(parsed[1]).divide(new BigDecimal(1000));
            Metrics metrics = new Metrics(time, null, value, BigDecimal.ZERO, label);
            metricscharger.add(metrics);
        }

        for(int i = 0; i < metricscharger.size() - 1; i++) {
            Metrics m1 = metricscharger.get(i);
            Metrics m2 = metricscharger.get(i + 1);
            ZonedDateTime time = m1.getTimestamp();
            BigDecimal value = m2.getValuet1().subtract(m1.getValuet1());
            Metrics metrics = new Metrics(m1.getTimestamp(), null, inPeakHours(time) ? value : BigDecimal.ZERO, inPeakHours(time) ? BigDecimal.ZERO : value, m1.getLabel());
            toCharger.add(metrics);
        }

        return toCharger;
    }

    private boolean inPeakHours(ZonedDateTime time) {
        if (time.getDayOfWeek().equals(DayOfWeek.SATURDAY) || time.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return false;
        }
        if (time.getHour() >= 22 || time.getHour() < 7) {
            return false;
        }
        return true;
    }



}
