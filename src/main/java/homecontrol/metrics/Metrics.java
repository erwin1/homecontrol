package homecontrol.metrics;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class Metrics {
    private ZonedDateTime timestamp;
    private String period;
    private BigDecimal valuet1;
    private BigDecimal valuet2;

    public Metrics(ZonedDateTime timestamp, String period, BigDecimal valuet1, BigDecimal valuet2) {
        this.timestamp = timestamp;
        this.period = period;
        this.valuet1 = valuet1;
        this.valuet2 = valuet2;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getValuet1() {
        return valuet1;
    }

    public void setValuet1(BigDecimal valuet1) {
        this.valuet1 = valuet1;
    }

    public BigDecimal getValuet2() {
        return valuet2;
    }

    public void setValuet2(BigDecimal valuet2) {
        this.valuet2 = valuet2;
    }
}
