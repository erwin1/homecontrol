package evcharging.services;

import evcharging.impl.sma.SMAAuthException;

import java.time.ZonedDateTime;
import java.util.List;

public interface ElectricityMeter {

    PowerValues getCurrentValues();

    long getConsumptionMeterReadingAt(ZonedDateTime timestamp);

    long getCurrentConsumptionMeterReading();

    List<MeterReading> getFromGridUsagePer15minBetween(ZonedDateTime startTime, ZonedDateTime endTime);


}