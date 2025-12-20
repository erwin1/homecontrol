package homecontrol.metrics;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class LiveMetrics {
    private ZonedDateTime timestamp;
    private BigDecimal importW;
    private BigDecimal importAverageW;
    private BigDecimal exportW;
    private BigDecimal pvW;
    private BigDecimal evW;
    private int evBatteryLevel;
    private boolean evConnected;
    private String evConnectedName;
    private BigDecimal peakEstimateW;
    private BigDecimal monthlyPowerPeakW;
    private ZonedDateTime monthlyPowerPeakTimestamp;

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getImportW() {
        return importW;
    }

    public void setImportW(BigDecimal importW) {
        this.importW = importW;
    }

    public BigDecimal getExportW() {
        return exportW;
    }

    public void setExportW(BigDecimal exportW) {
        this.exportW = exportW;
    }

    public BigDecimal getPvW() {
        return pvW;
    }

    public void setPvW(BigDecimal pvW) {
        this.pvW = pvW;
    }

    public BigDecimal getEvW() {
        return evW;
    }

    public void setEvW(BigDecimal evW) {
        this.evW = evW;
    }

    public int getEvBatteryLevel() {
        return evBatteryLevel;
    }

    public void setEvBatteryLevel(int evBatteryLevel) {
        this.evBatteryLevel = evBatteryLevel;
    }

    public String getEvConnectedName() {
        return evConnectedName;
    }

    public void setEvConnectedName(String evConnectedName) {
        this.evConnectedName = evConnectedName;
    }

    public boolean isEvConnected() {
        return evConnected;
    }

    public BigDecimal getImportAverageW() {
        return importAverageW;
    }

    public void setImportAverageW(BigDecimal importAverageW) {
        this.importAverageW = importAverageW;
    }

    public void setEvConnected(boolean evConnected) {
        this.evConnected = evConnected;
    }

    public BigDecimal getMonthlyPowerPeakW() {
        return monthlyPowerPeakW;
    }

    public void setMonthlyPowerPeakW(BigDecimal monthlyPowerPeakW) {
        this.monthlyPowerPeakW = monthlyPowerPeakW;
    }

    public ZonedDateTime getMonthlyPowerPeakTimestamp() {
        return monthlyPowerPeakTimestamp;
    }

    public void setMonthlyPowerPeakTimestamp(ZonedDateTime monthlyPowerPeakTimestamp) {
        this.monthlyPowerPeakTimestamp = monthlyPowerPeakTimestamp;
    }

    public BigDecimal getPeakEstimateW() {
        return peakEstimateW;
    }

    public void setPeakEstimateW(BigDecimal peakEstimateW) {
        this.peakEstimateW = peakEstimateW;
    }
}
