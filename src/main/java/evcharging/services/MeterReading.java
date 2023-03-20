package evcharging.services;

import java.time.ZonedDateTime;

public class MeterReading {
    private ZonedDateTime timestamp;
    private int value;

    public MeterReading(ZonedDateTime timestamp, int value) {
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

    public int getValueInW() {
        return getValue() * 4;
    }
}
