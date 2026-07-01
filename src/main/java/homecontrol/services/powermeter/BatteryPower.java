package homecontrol.services.powermeter;

import java.util.List;

public class BatteryPower {
    private int activePower;
    private String mode;
    private List<String> permissions;

    public BatteryPower() {
    }

    public BatteryPower(int activePower, String mode, List<String> permissions) {
        this.activePower = activePower;
        this.mode = mode;
        this.permissions = permissions;
    }

    public int getActivePower() {
        return activePower;
    }

    public BatteryPower setActivePower(int activePower) {
        this.activePower = activePower;
        return this;
    }

    public String getMode() {
        return mode;
    }

    public BatteryPower setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public BatteryPower setPermissions(List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
