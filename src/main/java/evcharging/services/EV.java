package evcharging.services;

public interface EV {

    int getCurrentBatteryLevel();

    void requestPowerConsumptionChange(int powerW, Integer limit);

}