package homecontrol.impl.hwep1;

import java.math.BigDecimal;

public class Telegram {
    private String timestamp;

    private BigDecimal total_power_import_kwh;
    private BigDecimal total_power_import_t1_kwh;
    private BigDecimal total_power_import_t2_kwh;
    private BigDecimal total_power_export_kwh;
    private BigDecimal total_power_export_t1_kwh;
    private BigDecimal total_power_export_t2_kwh;

    private BigDecimal total_gas_m3;

    private Integer active_power_import_w;
    private Integer active_power_export_w;
    private BigDecimal active_voltage_v;
    private Integer active_power_average_w;
    private Integer montly_power_peak_w;
    private String montly_power_peak_timestamp;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

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

    public Integer getActive_power_average_w() {
        return active_power_average_w;
    }

    public void setActive_power_average_w(Integer active_power_average_w) {
        this.active_power_average_w = active_power_average_w;
    }

    public Integer getMontly_power_peak_w() {
        return montly_power_peak_w;
    }

    public void setMontly_power_peak_w(Integer montly_power_peak_w) {
        this.montly_power_peak_w = montly_power_peak_w;
    }

    public String getMontly_power_peak_timestamp() {
        return montly_power_peak_timestamp;
    }

    public void setMontly_power_peak_timestamp(String montly_power_peak_timestamp) {
        this.montly_power_peak_timestamp = montly_power_peak_timestamp;
    }

    public BigDecimal getActive_voltage_v() {
        return active_voltage_v;
    }

    public void setActive_voltage_v(BigDecimal active_voltage_v) {
        this.active_voltage_v = active_voltage_v;
    }

    public Integer getActive_power_import_w() {
        return active_power_import_w;
    }

    public void setActive_power_import_w(Integer active_power_import_w) {
        this.active_power_import_w = active_power_import_w;
    }

    public Integer getActive_power_export_w() {
        return active_power_export_w;
    }

    public void setActive_power_export_w(Integer active_power_export_w) {
        this.active_power_export_w = active_power_export_w;
    }

    @Override
    public String toString() {
        return "Telegram{" +
                "timestamp='" + timestamp + '\'' +
                ", total_power_import_kwh=" + total_power_import_kwh +
                ", total_power_import_t1_kwh=" + total_power_import_t1_kwh +
                ", total_power_import_t2_kwh=" + total_power_import_t2_kwh +
                ", total_power_export_kwh=" + total_power_export_kwh +
                ", total_power_export_t1_kwh=" + total_power_export_t1_kwh +
                ", total_power_export_t2_kwh=" + total_power_export_t2_kwh +
                ", total_gas_m3=" + total_gas_m3 +
                ", active_power_import_w=" + active_power_import_w +
                ", active_power_export_w=" + active_power_export_w +
                ", active_voltage_v=" + active_voltage_v +
                ", active_power_average_w=" + active_power_average_w +
                ", montly_power_peak_w=" + montly_power_peak_w +
                ", montly_power_peak_timestamp='" + montly_power_peak_timestamp + '\'' +
                '}';
    }
}
