package evcharging.impl.homewizard;

import java.math.BigDecimal;

public class HWMeterData {
    private BigDecimal total_power_import_kwh;
    private BigDecimal total_power_import_t1_kwh;
    private BigDecimal total_power_import_t2_kwh;
    private BigDecimal total_power_export_kwh;
    private BigDecimal total_power_export_t1_kwh;
    private BigDecimal total_power_export_t2_kwh;
    private BigDecimal active_power_w;
    private BigDecimal active_power_average_w;
    private BigDecimal montly_power_peak_w;
    private String montly_power_peak_timestamp;
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

    public BigDecimal getActive_power_w() {
        return active_power_w;
    }

    public void setActive_power_w(BigDecimal active_power_w) {
        this.active_power_w = active_power_w;
    }

    public BigDecimal getActive_power_average_w() {
        return active_power_average_w;
    }

    public void setActive_power_average_w(BigDecimal active_power_average_w) {
        this.active_power_average_w = active_power_average_w;
    }

    public BigDecimal getMontly_power_peak_w() {
        return montly_power_peak_w;
    }

    public void setMontly_power_peak_w(BigDecimal montly_power_peak_w) {
        this.montly_power_peak_w = montly_power_peak_w;
    }

    public String getMontly_power_peak_timestamp() {
        return montly_power_peak_timestamp;
    }

    public void setMontly_power_peak_timestamp(String montly_power_peak_timestamp) {
        this.montly_power_peak_timestamp = montly_power_peak_timestamp;
    }

    public BigDecimal getTotal_gas_m3() {
        return total_gas_m3;
    }

    public void setTotal_gas_m3(BigDecimal total_gas_m3) {
        this.total_gas_m3 = total_gas_m3;
    }
}
