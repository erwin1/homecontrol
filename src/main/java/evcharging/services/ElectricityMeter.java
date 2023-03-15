package evcharging.services;

import java.time.ZonedDateTime;

public interface ElectricityMeter {

    PowerValues getCurrentValues();

    long getConsumptionMeterReadingAt(ZonedDateTime timestamp);

    long getCurrentConsumptionMeterReading();

}