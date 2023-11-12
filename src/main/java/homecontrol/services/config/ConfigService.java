package homecontrol.services.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
public class ConfigService {
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

    @ConfigProperty(name = "minimumPVYield")
    int minimumPVYield;

    @ConfigProperty(name = "minimumExport")
    int minimumExport;

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

    public int getMinimumPVYield() {
        return minimumPVYield;
    }

    public void setMinimumPVYield(int minimumPVYield) {
        this.minimumPVYield = minimumPVYield;
    }

    public int getMinimumExport() {
        return minimumExport;
    }

    public void setMinimumExport(int minimumExport) {
        this.minimumExport = minimumExport;
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

    public EVChargingConfigs getEVChargingConfigs() {
        EVChargingConfigs evChargingConfig = new EVChargingConfigs();

        EVChargingStrategy p1 = new EVChargingStrategy(getP1(), getMinimumPVYield(), getMinimumExport());
        EVChargingStrategy p2 = new EVChargingStrategy(getP2(), getMinimumPVYield(), getMinimumExport());
        EVChargingStrategy p3 = new EVChargingStrategy(getP3(), getMinimumPVYield(), getMinimumExport());
        EVChargingStrategy o1 = new EVChargingStrategy(getO1(), getMinimumPVYield(), getMinimumExport());
        EVChargingStrategy o2 = new EVChargingStrategy(getO2(), getMinimumPVYield(), getMinimumExport());
        EVChargingStrategy o3 = new EVChargingStrategy(getO3(), getMinimumPVYield(), getMinimumExport());

        evChargingConfig.addConfig(getBatteryLevelX(), TimeType.PEAK, p1);
        evChargingConfig.addConfig(getBatteryLevelX(), TimeType.OFF, o1);

        evChargingConfig.addConfig(getBatteryLevelY(), TimeType.PEAK, p2);
        evChargingConfig.addConfig(getBatteryLevelY(), TimeType.OFF, o2);

        evChargingConfig.addConfig(100, TimeType.PEAK, p3);
        evChargingConfig.addConfig(100, TimeType.OFF, o3);

        return evChargingConfig;
    }

    public void persist() {
        try {
            Properties properties = new Properties();
            properties.load(new FileReader("config/application.properties"));
            System.out.println("properties = " + properties);
            properties.setProperty("batteryLevelX", String.valueOf(getBatteryLevelX()));
            properties.setProperty("batteryLevelY", String.valueOf(getBatteryLevelY()));
            properties.setProperty("minimumPVYield", String.valueOf(getMinimumPVYield()));
            properties.setProperty("minimumExport", String.valueOf(getMinimumExport()));
            properties.setProperty("p1", String.valueOf(getP1()));
            properties.setProperty("p2", String.valueOf(getP2()));
            properties.setProperty("p3", String.valueOf(getP3()));
            properties.setProperty("o1", String.valueOf(getO1()));
            properties.setProperty("o2", String.valueOf(getO2()));
            properties.setProperty("o3", String.valueOf(getO3()));

            properties.setProperty("peakstrategy", String.valueOf(peakStrategy));
            properties.setProperty("minimumChargingA", String.valueOf(minimumChargingA));
            properties.setProperty("mode", String.valueOf(currentMode));
            properties.store(new FileWriter("config/application.properties"), "config changed by app");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
