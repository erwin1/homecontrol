package evcharging.services;

public interface EV {

    boolean isChargingComplete();

    void requestPowerConsumptionChange(int powerW);

}