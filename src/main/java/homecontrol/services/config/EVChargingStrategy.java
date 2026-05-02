package homecontrol.services.config;

public class EVChargingStrategy {
    public enum Type {
        EXP,
        EXP_OPT1,
        EXP_OPT2,
        MAX,
        MAX_OPT;
    }


    Type type;
    int minSolarYieldW;
    int minPowerExportW;
    int maxHoldAtMinCharging;

    public EVChargingStrategy(Type type) {
        this.type = type;
    }

    public EVChargingStrategy(Type type, int minSolarYieldW, int minPowerExportW, int maxHoldAtMinCharging) {
        this.type = type;
        this.minSolarYieldW = minSolarYieldW;
        this.minPowerExportW = minPowerExportW;
        this.maxHoldAtMinCharging = maxHoldAtMinCharging;
    }

    public Type getType() {
        return type;
    }

    public int getMinSolarYieldW() {
        return minSolarYieldW;
    }

    public int getMinPowerExportW() {
        return minPowerExportW;
    }

    public int getMaxHoldAtMinCharging() {
        return maxHoldAtMinCharging;
    }

    @Override
    public String toString() {
        return "EVChargingStrategy{" +
                "type=" + type +
                (type.equals(Type.EXP_OPT1) || type.equals(Type.EXP_OPT2) ? ", maxHoldAtMinCharging=" + maxHoldAtMinCharging : "") +
                (type.equals(Type.EXP_OPT1) || type.equals(Type.EXP_OPT2) ? ", minSolarYieldW=" + minSolarYieldW : "") +
                (type.equals(Type.EXP_OPT1) || type.equals(Type.EXP_OPT2) ? ", minPowerExportW=" + minPowerExportW : "") +
                '}';
    }
}
