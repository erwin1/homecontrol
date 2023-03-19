package evcharging.services;

public interface EV {

    boolean isChargingComplete();

    int getCurrentBatteryLevel();

    void requestPowerConsumptionChange(int powerW, Integer limit);

}