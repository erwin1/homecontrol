package homecontrol.services.config;

public class EVChargingConfig {
    private int maxBatteryLevel;
    private TimeType timeType;
    private EVChargingStrategy chargingStrategy;

    public EVChargingConfig(int maxBatteryLevel, TimeType timeType, EVChargingStrategy chargingStrategy) {
        this.maxBatteryLevel = maxBatteryLevel;
        this.timeType = timeType;
        this.chargingStrategy = chargingStrategy;
    }

    public int getMaxBatteryLevel() {
        return maxBatteryLevel;
    }

    public TimeType getTimeType() {
        return timeType;
    }


    public EVChargingStrategy getChargingStrategy() {
        return chargingStrategy;
    }
}
