package homecontrol.services.config;

import homecontrol.services.notications.NotificationType;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConfigService {
    private static final Logger LOGGER = Logger.getLogger(ConfigService.class.getName());
    private final TreeMap<LocalDate, BigDecimal> mutableTariffs = new TreeMap<>();
    @ConfigProperty(name = "max15minpeak")
    int max15minPeak;

    @ConfigProperty(name = "min15minpeak")
    int min15minPeak;

    @ConfigProperty(name = "peakstrategy")
    PeakStrategy peakStrategy;

    @ConfigProperty(name = "batteryLevelX")
    int batteryLevelX;

    @ConfigProperty(name = "batteryLevelY")
    int batteryLevelY;

    @ConfigProperty(name = "minimumPVYieldExp1")
    int minimumPVYieldExp1;

    @ConfigProperty(name = "minimumExportExp1")
    int minimumExportExp1;

    @ConfigProperty(name = "maxHoldAtMinChargingExp1", defaultValue = "2")
    int maxHoldAtMinChargingExp1;

    @ConfigProperty(name = "minimumPVYieldExp2")
    int minimumPVYieldExp2;

    @ConfigProperty(name = "minimumExportExp2")
    int minimumExportExp2;

    @ConfigProperty(name = "maxHoldAtMinChargingExp2", defaultValue = "2")
    int maxHoldAtMinChargingExp2;

    @ConfigProperty(name = "p1")
    EVChargingStrategy.Type p1;
    @ConfigProperty(name = "p2")
    EVChargingStrategy.Type p2;
    @ConfigProperty(name = "p3")
    EVChargingStrategy.Type p3;
    @ConfigProperty(name = "o1")
    EVChargingStrategy.Type o1;
    @ConfigProperty(name = "o2")
    EVChargingStrategy.Type o2;
    @ConfigProperty(name = "o3")
    EVChargingStrategy.Type o3;

    @ConfigProperty(name = "mode")
    Mode currentMode;

    @ConfigProperty(name = "minimumChargingA")
    int minimumChargingA;

    @ConfigProperty(name = "tariff")
    Optional<Map<String, String>> tariffConfig;

    @ConfigProperty(name = "enabledNotificationTypes", defaultValue = "ERROR,INFO,SUMMARY,PEAK_WARNING,DEBUG")
    Optional<String> enabledNotificationTypesRaw;

    private Set<NotificationType> enabledNotificationTypes;

    @PostConstruct
    void initTariffs() {
        tariffConfig.ifPresent(map -> map.forEach((key, value) -> {
            try {
                mutableTariffs.put(LocalDate.parse(key), new BigDecimal(value));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping invalid tariff entry: " + key + "=" + value, e);
            }
        }));
        enabledNotificationTypes = enabledNotificationTypesRaw
                .filter(s -> !s.isEmpty())
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim).filter(t -> !t.isEmpty())
                        .map(NotificationType::valueOf)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class))))
                .orElse(EnumSet.noneOf(NotificationType.class));
    }


    public Mode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(Mode currentMode) {
        this.currentMode = currentMode;
    }

    public int getMax15minPeak() {
        return max15minPeak;
    }

    public void setMax15minPeak(int max15minPeak) {
        this.max15minPeak = max15minPeak;
    }

    public int getMin15minPeak() {
        return min15minPeak;
    }

    public void setMin15minPeak(int min15minPeak) {
        this.min15minPeak = min15minPeak;
    }

    public PeakStrategy getPeakStrategy() {
        return peakStrategy;
    }

    public void setPeakStrategy(PeakStrategy peakStrategy) {
        this.peakStrategy = peakStrategy;
    }

    public int getBatteryLevelX() {
        return batteryLevelX;
    }

    public void setBatteryLevelX(int batteryLevelX) {
        this.batteryLevelX = batteryLevelX;
    }

    public int getBatteryLevelY() {
        return batteryLevelY;
    }

    public void setBatteryLevelY(int batteryLevelY) {
        this.batteryLevelY = batteryLevelY;
    }

    public int getMinimumPVYieldExp1() {
        return minimumPVYieldExp1;
    }

    public void setMinimumPVYieldExp1(int minimumPVYieldExp1) {
        this.minimumPVYieldExp1 = minimumPVYieldExp1;
    }

    public int getMinimumExportExp1() {
        return minimumExportExp1;
    }

    public void setMinimumExportExp1(int minimumExportExp1) {
        this.minimumExportExp1 = minimumExportExp1;
    }

    public int getMaxHoldAtMinChargingExp1() {
        return maxHoldAtMinChargingExp1;
    }

    public void setMaxHoldAtMinChargingExp1(int maxHoldAtMinChargingExp1) {
        this.maxHoldAtMinChargingExp1 = maxHoldAtMinChargingExp1;
    }

    public int getMinimumPVYieldExp2() {
        return minimumPVYieldExp2;
    }

    public void setMinimumPVYieldExp2(int minimumPVYieldExp2) {
        this.minimumPVYieldExp2 = minimumPVYieldExp2;
    }

    public int getMinimumExportExp2() {
        return minimumExportExp2;
    }

    public void setMinimumExportExp2(int minimumExportExp2) {
        this.minimumExportExp2 = minimumExportExp2;
    }

    public int getMaxHoldAtMinChargingExp2() {
        return maxHoldAtMinChargingExp2;
    }

    public void setMaxHoldAtMinChargingExp2(int maxHoldAtMinChargingExp2) {
        this.maxHoldAtMinChargingExp2 = maxHoldAtMinChargingExp2;
    }

    public EVChargingStrategy.Type getP1() {
        return p1;
    }

    public void setP1(EVChargingStrategy.Type p1) {
        this.p1 = p1;
    }

    public EVChargingStrategy.Type getP2() {
        return p2;
    }

    public void setP2(EVChargingStrategy.Type p2) {
        this.p2 = p2;
    }

    public EVChargingStrategy.Type getP3() {
        return p3;
    }

    public void setP3(EVChargingStrategy.Type p3) {
        this.p3 = p3;
    }

    public EVChargingStrategy.Type getO1() {
        return o1;
    }

    public void setO1(EVChargingStrategy.Type o1) {
        this.o1 = o1;
    }

    public EVChargingStrategy.Type getO2() {
        return o2;
    }

    public void setO2(EVChargingStrategy.Type o2) {
        this.o2 = o2;
    }

    public EVChargingStrategy.Type getO3() {
        return o3;
    }

    public void setO3(EVChargingStrategy.Type o3) {
        this.o3 = o3;
    }

    public int getMinimumChargingA() {
        return minimumChargingA;
    }

    public void setMinimumChargingA(int minimumChargingA) {
        this.minimumChargingA = minimumChargingA;
    }

    public Set<NotificationType> getEnabledNotificationTypes() {
        return enabledNotificationTypes;
    }

    public void setEnabledNotificationTypes(Set<NotificationType> enabledNotificationTypes) {
        this.enabledNotificationTypes = enabledNotificationTypes;
    }


    public EVChargingConfigs getEVChargingConfigs() {
        EVChargingConfigs evChargingConfig = new EVChargingConfigs();

        EVChargingStrategy p1, p2, p3, o1, o2, o3;
        if (getP1().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            p1 = new EVChargingStrategy(getP1(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getP1().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            p1 = new EVChargingStrategy(getP1(), getMinimumPVYieldExp2(), getMinimumExportExp2(), getMaxHoldAtMinChargingExp2());
        } else {
            p1 = new EVChargingStrategy(getP1());
        }
        if (getP2().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            p2 = new EVChargingStrategy(getP2(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getP1().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            p2 = new EVChargingStrategy(getP2(), getMinimumPVYieldExp2(), getMinimumExportExp2(), getMaxHoldAtMinChargingExp2());
        } else {
            p2 = new EVChargingStrategy(getP2());
        }
        if (getP3().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            p3 = new EVChargingStrategy(getP3(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getP3().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            p3 = new EVChargingStrategy(getP3(), getMinimumPVYieldExp2(), getMinimumExportExp2(), getMaxHoldAtMinChargingExp2());
        } else {
            p3 = new EVChargingStrategy(getP3());
        }
        if (getO1().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            o1 = new EVChargingStrategy(getO1(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getP1().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            o1 = new EVChargingStrategy(getO1(), getMinimumPVYieldExp2(), getMinimumExportExp2(), getMaxHoldAtMinChargingExp2());
        } else {
            o1 = new EVChargingStrategy(getO1());
        }
        if (getO2().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            o2 = new EVChargingStrategy(getO2(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getO1().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            o2 = new EVChargingStrategy(getO2(), getMinimumPVYieldExp2(), getMinimumExportExp2(),getMaxHoldAtMinChargingExp2());
        } else {
            o2 = new EVChargingStrategy(getO2());
        }
        if (getO3().equals(EVChargingStrategy.Type.EXP_OPT1)) {
            o3 = new EVChargingStrategy(getO3(), getMinimumPVYieldExp1(), getMinimumExportExp1(), getMaxHoldAtMinChargingExp1());
        } else if (getO3().equals(EVChargingStrategy.Type.EXP_OPT2)) {
            o3 = new EVChargingStrategy(getO3(), getMinimumPVYieldExp2(), getMinimumExportExp2(), getMaxHoldAtMinChargingExp2());
        } else {
            o3 = new EVChargingStrategy(getO3());
        }

        evChargingConfig.addConfig(getBatteryLevelX(), TimeType.PEAK, p1);
        evChargingConfig.addConfig(getBatteryLevelX(), TimeType.OFF, o1);

        evChargingConfig.addConfig(getBatteryLevelY(), TimeType.PEAK, p2);
        evChargingConfig.addConfig(getBatteryLevelY(), TimeType.OFF, o2);

        evChargingConfig.addConfig(100, TimeType.PEAK, p3);
        evChargingConfig.addConfig(100, TimeType.OFF, o3);

        return evChargingConfig;
    }

    public Optional<BigDecimal> getTariffFor(LocalDate month) {
        Map.Entry<LocalDate, BigDecimal> entry = mutableTariffs.floorEntry(month);
        return Optional.ofNullable(entry).map(Map.Entry::getValue);
    }

    public void setTariff(LocalDate effectiveFrom, BigDecimal value) {
        mutableTariffs.put(effectiveFrom, value);
    }

    public List<Map.Entry<LocalDate, BigDecimal>> getTariffEntries() {
        return new ArrayList<>(mutableTariffs.entrySet());
    }

    public void persist() {
        try {
            Properties properties = new Properties();
            properties.load(new FileReader("config/application.properties"));
            System.out.println("properties = " + properties);
            properties.setProperty("batteryLevelX", String.valueOf(getBatteryLevelX()));
            properties.setProperty("batteryLevelY", String.valueOf(getBatteryLevelY()));
            properties.setProperty("minimumPVYieldExp1", String.valueOf(getMinimumPVYieldExp1()));
            properties.setProperty("minimumExportExp1", String.valueOf(getMinimumExportExp1()));
            properties.setProperty("maxHoldAtMinChargingExp1", String.valueOf(getMaxHoldAtMinChargingExp1()));
            properties.setProperty("minimumPVYieldExp2", String.valueOf(getMinimumPVYieldExp2()));
            properties.setProperty("minimumExportExp2", String.valueOf(getMinimumExportExp2()));
            properties.setProperty("maxHoldAtMinChargingExp2", String.valueOf(getMaxHoldAtMinChargingExp2()));
            properties.setProperty("p1", String.valueOf(getP1()));
            properties.setProperty("p2", String.valueOf(getP2()));
            properties.setProperty("p3", String.valueOf(getP3()));
            properties.setProperty("o1", String.valueOf(getO1()));
            properties.setProperty("o2", String.valueOf(getO2()));
            properties.setProperty("o3", String.valueOf(getO3()));

            properties.setProperty("peakstrategy", String.valueOf(peakStrategy));
            properties.setProperty("minimumChargingA", String.valueOf(minimumChargingA));
            properties.setProperty("mode", String.valueOf(currentMode));
            properties.setProperty("enabledNotificationTypes",
                    enabledNotificationTypes.stream().map(Enum::name).collect(Collectors.joining(",")));
            properties.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("tariff."))
                    .collect(Collectors.toList())
                    .forEach(properties::remove);
            mutableTariffs.forEach((date, value) ->
                    properties.setProperty("tariff." + date, value.toPlainString()));
            properties.store(new FileWriter("config/application.properties"), "config changed by app");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
