package homecontrol.services.powermeter;

import io.smallrye.mutiny.Uni;

public interface ElectricalPowerMeter {

    MeterReading getCurrentReading();

    Uni<ActivePower> getActivePower();

    Uni<Power> getPower();

    MonthlyPowerPeak getMonthlyPowerPeak();

}
