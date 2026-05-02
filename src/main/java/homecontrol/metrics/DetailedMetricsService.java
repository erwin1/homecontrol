package homecontrol.metrics;

import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.Power;
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
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@ApplicationScoped
public class DetailedMetricsService {
    public static final Logger LOGGER = Logger.getLogger(DetailedMetricsService.class.getName());

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");

    @ConfigProperty(name = "METRICSLOGGER_DATA")
    Optional<String> dataPath;

    @Inject
    private ElectricalPowerMeter electricalPowerMeter;

    List<Metrics> gridImport;
    List<Metrics> gridExport;

    @PostConstruct
    void readFiles() throws IOException {
        LOGGER.info("initializing detailed metrics service");
        gridImport = new ArrayList<>();
        gridExport = new ArrayList<>();

        if (!dataPath.isPresent()) {
            LOGGER.warning("no data path configured, detailed metrics will not be persisted");
            return;
        }

        ZonedDateTime cutoff = ZonedDateTime.now().minus(Duration.ofHours(24));
        Path importFile = Path.of(dataPath.get()).resolve("detailed_import.csv");
        Path exportFile = Path.of(dataPath.get()).resolve("detailed_export.csv");

        if (Files.exists(importFile)) {
            for (String line : Files.readAllLines(importFile)) {
                Metrics m = parseLine(line);
                if (m != null && !m.getTimestamp().isBefore(cutoff)) {
                    gridImport.add(m);
                }
            }
            rewriteFile(importFile, gridImport);
        }
        if (Files.exists(exportFile)) {
            for (String line : Files.readAllLines(exportFile)) {
                Metrics m = parseLine(line);
                if (m != null && !m.getTimestamp().isBefore(cutoff)) {
                    gridExport.add(m);
                }
            }
            rewriteFile(exportFile, gridExport);
        }

        LOGGER.info("initializing detailed metrics service done");
    }

    @Scheduled(every = "1m", concurrentExecution = SKIP)
    public void retrieveDetailedMetrics() {
        Power power = electricalPowerMeter.getPower().await().atMost(Duration.ofSeconds(10));

        ZonedDateTime ts = ZonedDateTime.now();
        BigDecimal readingImportT1 = power.getMeterReading().getTotal_power_import_t1_kwh();
        BigDecimal readingImportT2 = power.getMeterReading().getTotal_power_import_t2_kwh();
        BigDecimal readingExportT1 = power.getMeterReading().getTotal_power_export_t1_kwh();
        BigDecimal readingExportT2 = power.getMeterReading().getTotal_power_export_t2_kwh();

        gridImport.add(new Metrics(ts, "minute", readingImportT1, readingImportT2));
        gridExport.add(new Metrics(ts, "minute", readingExportT1, readingExportT2));

        if (dataPath.isPresent()) {
            String tsString = ts.withZoneSameInstant(BRUSSELS).format(FORMATTER);
            Path path = Path.of(dataPath.get());
            try (BufferedWriter w = Files.newBufferedWriter(path.resolve("detailed_import.csv"), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(tsString + "," + readingImportT1 + "," + readingImportT2 + "\n");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to write detailed import metrics", e);
            }
            try (BufferedWriter w = Files.newBufferedWriter(path.resolve("detailed_export.csv"), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(tsString + "," + readingExportT1 + "," + readingExportT2 + "\n");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to write detailed export metrics", e);
            }
        }

        ZonedDateTime cutoff = ts.minus(Duration.ofHours(24));
        gridImport.removeIf(m -> m.getTimestamp().isBefore(cutoff));
        gridExport.removeIf(m -> m.getTimestamp().isBefore(cutoff));

        LOGGER.info("Usage in last 5 minutes: "+getRecentImport(5));
        LOGGER.info("Export in last 5 minutes: "+getRecentExport(5));
    }

    /**
     * Returns the combined t1 and t2 import in the specified duration
     */
    public BigDecimal getRecentImport(int minutes) {
        return computeDelta(gridImport, Duration.ofMinutes(minutes));
    }

    /**
     * Returns the combined t1 and t2 export in the specified duration
     */
    public BigDecimal getRecentExport(int minutes) {
        return computeDelta(gridExport, Duration.ofMinutes(minutes));
    }

    private BigDecimal computeDelta(List<Metrics> readings, Duration duration) {
        if (readings.size() < 2) return BigDecimal.ZERO;
        ZonedDateTime cutoff = ZonedDateTime.now().minus(duration);
        Metrics oldest = null;
        for (Metrics m : readings) {
            if (!m.getTimestamp().isBefore(cutoff)) {
                oldest = m;
                break;
            }
        }
        if (oldest == null) return BigDecimal.ZERO;
        Metrics newest = readings.get(readings.size() - 1);
        BigDecimal delta = newest.getValuet1().subtract(oldest.getValuet1())
                .add(newest.getValuet2().subtract(oldest.getValuet2()));
        return delta.max(BigDecimal.ZERO);
    }

    private void rewriteFile(Path file, List<Metrics> entries) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Metrics m : entries) {
                w.write(m.getTimestamp().withZoneSameInstant(BRUSSELS).format(FORMATTER)
                        + "," + m.getValuet1() + "," + m.getValuet2() + "\n");
            }
        }
    }

    private Metrics parseLine(String line) {
        try {
            String[] parts = line.split(",");
            ZonedDateTime ts = ZonedDateTime.parse(parts[0], FORMATTER).withZoneSameInstant(BRUSSELS);
            return new Metrics(ts, "minute", new BigDecimal(parts[1]), new BigDecimal(parts[2]));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Skipping malformed line: " + line, e);
            return null;
        }
    }
}
