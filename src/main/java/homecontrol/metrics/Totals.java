package homecontrol.metrics;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class Totals {
    private ZonedDateTime timestamp;
    private List<CombinedMetrics> details;
    private CombinedMetrics totals;
    private Map<String, BigDecimal> evPerLabel;

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<CombinedMetrics> getDetails() {
        return details;
    }

    public void setDetails(List<CombinedMetrics> details) {
        this.details = details;
    }

    public CombinedMetrics getTotals() {
        return totals;
    }

    public void setTotals(CombinedMetrics totals) {
        this.totals = totals;
    }

    public Map<String, BigDecimal> getEvPerLabel() {
        return evPerLabel;
    }

    public void setEvPerLabel(Map<String, BigDecimal> evPerLabel) {
        this.evPerLabel = evPerLabel;
    }
}
