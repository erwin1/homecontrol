package homecontrol.services.config;

import java.util.LinkedList;
import java.util.List;

public class EVChargingConfigs {

    private List<EVChargingConfig> configs = new LinkedList<>();

    public EVChargingStrategy getEVChargingStrategy(int batteryLevel, TimeType timeType) {
        return configs.stream()
                .filter(x -> batteryLevel < x.getMaxBatteryLevel())
                .filter(x -> x.getTimeType().equals(timeType))
                .findFirst()
                .orElse(new EVChargingConfig(100, timeType, new EVChargingStrategy(EVChargingStrategy.Type.EXP)))
                .getChargingStrategy();
    }

    public void addConfig(int batteryLevel, TimeType timeType, EVChargingStrategy evChargingStrategy) {
        configs.add(new EVChargingConfig(batteryLevel, timeType, evChargingStrategy));
    }






}
