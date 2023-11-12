package homecontrol.metrics;

import java.time.ZonedDateTime;
import java.util.List;

public class Totals {
    private ZonedDateTime timestamp;
    private List<CombinedMetrics> details;
    private CombinedMetrics totals;

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
}
