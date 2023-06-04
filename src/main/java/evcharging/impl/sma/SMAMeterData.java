package evcharging.impl.sma;

public class SMAMeterData {
    private int toGrid;
    private int fromGrid;
    private int fromPV;

    public SMAMeterData() {
    }

    public SMAMeterData(int toGrid, int fromGrid, int fromPV) {
        this.toGrid = toGrid;
        this.fromGrid = fromGrid;
        this.fromPV = fromPV;
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

}
