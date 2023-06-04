package evcharging.services;

public interface ElectricityMeter {

    MeterData getLivePowerData();

    MeterReading getCurrentMonthPeak();

}