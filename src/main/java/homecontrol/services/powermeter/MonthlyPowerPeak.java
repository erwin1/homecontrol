package homecontrol.services.powermeter;

import java.time.ZonedDateTime;

public class MonthlyPowerPeak {
    private ZonedDateTime timestamp;
    private int value;

    public MonthlyPowerPeak(ZonedDateTime timestamp, int value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
