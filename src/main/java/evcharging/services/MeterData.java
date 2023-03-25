package evcharging.services;


public class MeterData {
    int activePowerW;
    int activePowerAverageW;
    long totalPowerImportWH;

    public MeterData(int activePowerW, int activePowerAverageW, long totalPowerImportWH) {
        this.activePowerW = activePowerW;
        this.activePowerAverageW = activePowerAverageW;
        this.totalPowerImportWH = totalPowerImportWH;
    }

    public int getActivePowerW() {
        return activePowerW;
    }

    public int getActivePowerAverageW() {
        return activePowerAverageW;
    }

    public long getTotalPowerImportWH() {
        return totalPowerImportWH;
    }
}
