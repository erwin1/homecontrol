package homecontrol.services.powermeter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class ActivePower {
    private ZonedDateTime timestamp;
    private int activePower;
    private int activePowerAverage;
    private BigDecimal activeVoltage;

    public ActivePower() {
    }

    public ActivePower(ZonedDateTime timestamp, int activePower, int activePowerAverage, BigDecimal activeVoltage) {
        this.timestamp = timestamp;
        this.activePower = activePower;
        this.activePowerAverage = activePowerAverage;
        this.activeVoltage = activeVoltage;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getActivePower() {
        return activePower;
    }

    public void setActivePower(int activePower) {
        this.activePower = activePower;
    }

    public int getActivePowerAverage() {
        return activePowerAverage;
    }

    public void setActivePowerAverage(int activePowerAverage) {
        this.activePowerAverage = activePowerAverage;
    }

    public BigDecimal getActiveVoltage() {
        return activeVoltage;
    }

    public void setActiveVoltage(BigDecimal activeVoltage) {
        this.activeVoltage = activeVoltage;
    }
}
