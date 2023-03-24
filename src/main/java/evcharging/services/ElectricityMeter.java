package evcharging.services;

public interface ElectricityMeter {

    MeterData getCurrentData();

    MeterReading getCurrentMonthPeak();

}