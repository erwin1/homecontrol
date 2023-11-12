package homecontrol.services.config;

public class EVChargingStrategy {
    public enum Type {
        EXP,
        EXP_PLUS,
        MAX,
        MAX_OPT;
    }


    Type type;
    int minSolarYieldW;
    int minPowerExportW;

    public EVChargingStrategy(Type type) {
        this.type = type;
    }

    public EVChargingStrategy(Type type, int minSolarYieldW, int minPowerExportW) {
        this.type = type;
        this.minSolarYieldW = minSolarYieldW;
        this.minPowerExportW = minPowerExportW;
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

    @Override
    public String toString() {
        return "EVChargingStrategy{" +
                "type=" + type +
                (type.equals(Type.EXP_PLUS) ? ", minSolarYieldW=" + minSolarYieldW : "") +
                (type.equals(Type.EXP_PLUS) ? ", minPowerExportW=" + minPowerExportW : "") +
                '}';
    }
}
