package homecontrol.services.powermeter;

import java.math.BigDecimal;

public class BatteryMeterReading {
    private BigDecimal energy_import_kwh;
    private BigDecimal energy_export_kwh;
    private int state_of_charge_pct;
    private int cycles;

    public BigDecimal getEnergy_import_kwh() {
        return energy_import_kwh;
    }

    public BatteryMeterReading setEnergy_import_kwh(BigDecimal energy_import_kwh) {
        this.energy_import_kwh = energy_import_kwh;
        return this;
    }

    public BigDecimal getEnergy_export_kwh() {
        return energy_export_kwh;
    }

    public BatteryMeterReading setEnergy_export_kwh(BigDecimal energy_export_kwh) {
        this.energy_export_kwh = energy_export_kwh;
        return this;
    }

    public int getState_of_charge_pct() {
        return state_of_charge_pct;
    }

    public BatteryMeterReading setState_of_charge_pct(int state_of_charge_pct) {
        this.state_of_charge_pct = state_of_charge_pct;
        return this;
    }

    public int getCycles() {
        return cycles;
    }

    public BatteryMeterReading setCycles(int cycles) {
        this.cycles = cycles;
        return this;
    }
}
