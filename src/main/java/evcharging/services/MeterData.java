package evcharging.services;


public class MeterData {
    int activePowerW;
    int activePowerAverageW;

    public MeterData(int activePowerW, int activePowerAverageW) {
        this.activePowerW = activePowerW;
        this.activePowerAverageW = activePowerAverageW;
    }

    public int getActivePowerW() {
        return activePowerW;
    }

    public int getActivePowerAverageW() {
        return activePowerAverageW;
    }
}
