package evcharging.services;

import java.time.LocalDateTime;

public class PowerValues {
    private int toGrid;
    private int fromGrid;
    private int fromPV;
    private int totalConsumption;
    private LocalDateTime timestamp;

    public PowerValues() {
    }

    public PowerValues(int toGrid, int fromGrid, int fromPV, int totalConsumption) {
        this.toGrid = toGrid;
        this.fromGrid = fromGrid;
        this.fromPV = fromPV;
        this.totalConsumption = totalConsumption;
        this.timestamp = LocalDateTime.now();
    }

    public int getToGrid() {
        return toGrid;
    }

    public void setToGrid(int toGrid) {
        this.toGrid = toGrid;
    }

    public int getFromGrid() {
        return fromGrid;
    }

    public void setFromGrid(int fromGrid) {
        this.fromGrid = fromGrid;
    }

    public int getFromPV() {
        return fromPV;
    }

    public void setFromPV(int fromPV) {
        this.fromPV = fromPV;
    }

    public int getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(int totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Electricity{" +
                "toGrid=" + toGrid +
                ", fromGrid=" + fromGrid +
                ", fromPV=" + fromPV +
                ", totalConsumption=" + totalConsumption +
                '}';
    }
}
