package homecontrol.services.powermeter;

public class Power {
    private ActivePower activePower;
    private MeterReading meterReading;
    private MonthlyPowerPeak monthlyPowerPeak;

    public Power() {
    }

    public Power(ActivePower activePower, MeterReading meterReading, MonthlyPowerPeak monthlyPowerPeak) {
        this.activePower = activePower;
        this.meterReading = meterReading;
        this.monthlyPowerPeak = monthlyPowerPeak;
    }

    public ActivePower getActivePower() {
        return activePower;
    }

    public void setActivePower(ActivePower activePower) {
        this.activePower = activePower;
    }

    public MeterReading getMeterReading() {
        return meterReading;
    }

    public void setMeterReading(MeterReading meterReading) {
        this.meterReading = meterReading;
    }

    public MonthlyPowerPeak getMonthlyPowerPeak() {
        return monthlyPowerPeak;
    }

    public void setMonthlyPowerPeak(MonthlyPowerPeak monthlyPowerPeak) {
        this.monthlyPowerPeak = monthlyPowerPeak;
    }
}
