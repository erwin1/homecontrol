package homecontrol.services.powermeter;

import java.math.BigDecimal;

public class MeterReading {
    private BigDecimal total_power_import_kwh;
    private BigDecimal total_power_import_t1_kwh;
    private BigDecimal total_power_import_t2_kwh;
    private BigDecimal total_power_export_kwh;
    private BigDecimal total_power_export_t1_kwh;
    private BigDecimal total_power_export_t2_kwh;

    private BigDecimal total_gas_m3;

    public BigDecimal getTotal_power_import_kwh() {
        return total_power_import_kwh;
    }

    public void setTotal_power_import_kwh(BigDecimal total_power_import_kwh) {
        this.total_power_import_kwh = total_power_import_kwh;
    }

    public BigDecimal getTotal_power_import_t1_kwh() {
        return total_power_import_t1_kwh;
    }

    public void setTotal_power_import_t1_kwh(BigDecimal total_power_import_t1_kwh) {
        this.total_power_import_t1_kwh = total_power_import_t1_kwh;
    }

    public BigDecimal getTotal_power_import_t2_kwh() {
        return total_power_import_t2_kwh;
    }

    public void setTotal_power_import_t2_kwh(BigDecimal total_power_import_t2_kwh) {
        this.total_power_import_t2_kwh = total_power_import_t2_kwh;
    }

    public BigDecimal getTotal_power_export_kwh() {
        return total_power_export_kwh;
    }

    public void setTotal_power_export_kwh(BigDecimal total_power_export_kwh) {
        this.total_power_export_kwh = total_power_export_kwh;
    }

    public BigDecimal getTotal_power_export_t1_kwh() {
        return total_power_export_t1_kwh;
    }

    public void setTotal_power_export_t1_kwh(BigDecimal total_power_export_t1_kwh) {
        this.total_power_export_t1_kwh = total_power_export_t1_kwh;
    }

    public BigDecimal getTotal_power_export_t2_kwh() {
        return total_power_export_t2_kwh;
    }

    public void setTotal_power_export_t2_kwh(BigDecimal total_power_export_t2_kwh) {
        this.total_power_export_t2_kwh = total_power_export_t2_kwh;
    }

    public BigDecimal getTotal_gas_m3() {
        return total_gas_m3;
    }

    public void setTotal_gas_m3(BigDecimal total_gas_m3) {
        this.total_gas_m3 = total_gas_m3;
    }
}
