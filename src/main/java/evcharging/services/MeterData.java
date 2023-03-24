package evcharging.services;


public class MeterData {
    int activePowerW;
    int activePowerAverageW;
    long totalPowerExportWH;

    public MeterData(int activePowerW, int activePowerAverageW, long totalPowerExportWH) {
        this.activePowerW = activePowerW;
        this.activePowerAverageW = activePowerAverageW;
        this.totalPowerExportWH = totalPowerExportWH;
    }

    public int getActivePowerW() {
        return activePowerW;
    }

    public int getActivePowerAverageW() {
        return activePowerAverageW;
    }

}
