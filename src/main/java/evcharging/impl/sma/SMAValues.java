package evcharging.impl.sma;

public class SMAValues {
    private int toGrid;
    private int fromGrid;
    private int fromPV;
    private int totalConsumption;

    public SMAValues() {
    }

    public SMAValues(int toGrid, int fromGrid, int fromPV, int totalConsumption) {
        this.toGrid = toGrid;
        this.fromGrid = fromGrid;
        this.fromPV = fromPV;
        this.totalConsumption = totalConsumption;
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

}
