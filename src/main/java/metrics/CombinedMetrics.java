package metrics;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class CombinedMetrics {
    private ZonedDateTime timestamp;
    private BigDecimal importt1;
    private BigDecimal importt2;
    private BigDecimal exportt1;
    private BigDecimal exportt2;
    private BigDecimal pvt1;
    private BigDecimal pvt2;
    private BigDecimal evt1;
    private BigDecimal evt2;

    public CombinedMetrics() {
        importt1 = BigDecimal.ZERO;
        importt2 = BigDecimal.ZERO;
        exportt1 = BigDecimal.ZERO;
        exportt2 = BigDecimal.ZERO;
        evt1 = BigDecimal.ZERO;
        evt2 = BigDecimal.ZERO;
        pvt1 = BigDecimal.ZERO;
        pvt2 = BigDecimal.ZERO;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getTotalUsage() {
        return getImport().subtract(getExport()).add(getPV());
    }

    public BigDecimal getTotalUsageWithoutEV() {
        return getTotalUsage().subtract(getEV());
    }

    public BigDecimal getImportt1() {
        return importt1;
    }

    public void setImportt1(BigDecimal importt1) {
        this.importt1 = importt1;
    }

    public BigDecimal getImportt2() {
        return importt2;
    }

    public void setImportt2(BigDecimal importt2) {
        this.importt2 = importt2;
    }

    public BigDecimal getExportt1() {
        return exportt1;
    }

    public void setExportt1(BigDecimal exportt1) {
        this.exportt1 = exportt1;
    }

    public BigDecimal getExportt2() {
        return exportt2;
    }

    public void setExportt2(BigDecimal exportt2) {
        this.exportt2 = exportt2;
    }

    public BigDecimal getPvt1() {
        return pvt1;
    }

    public void setPvt1(BigDecimal pvt1) {
        this.pvt1 = pvt1;
    }

    public BigDecimal getPvt2() {
        return pvt2;
    }

    public void setPvt2(BigDecimal pvt2) {
        this.pvt2 = pvt2;
    }

    public BigDecimal getEvt1() {
        return evt1;
    }

    public void setEvt1(BigDecimal evt1) {
        this.evt1 = evt1;
    }

    public BigDecimal getEvt2() {
        return evt2;
    }

    public void setEvt2(BigDecimal evt2) {
        this.evt2 = evt2;
    }

    public BigDecimal getImport() {
        return importt1.add(importt2);
    }

    public BigDecimal getExport() {
        return exportt1.add(exportt2);
    }

    public BigDecimal getPV() {
        return pvt1.add(pvt2);
    }

    public BigDecimal getEV() {
        return evt1.add(evt2);
    }

}
