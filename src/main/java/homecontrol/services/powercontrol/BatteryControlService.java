package homecontrol.services.powercontrol;

import homecontrol.services.powermeter.BatteryPower;
import homecontrol.services.powermeter.HomeBatteryPowerMeter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.LocalDateTime;

@ApplicationScoped
public class BatteryControlService {
    @Inject
    HomeBatteryPowerMeter homeBatteryPowerMeter;
    BatteryPower latestBatteryPower;

    public void set(BatteryPower batteryPower) {
        latestBatteryPower = batteryPower;
    }

    private void checkSet() {
        if (latestBatteryPower == null) {
            latestBatteryPower = homeBatteryPowerMeter.getActivePower().await().atMost(Duration.ofSeconds(10));
        }
    }

    public void disableDischarging() {
        checkSet();
        if (latestBatteryPower.getPermissions().contains("discharge_allowed")) {
            homeBatteryPowerMeter.changePermissions(false, true);
        }
    }

    public void reset() {
        checkSet();
        if (!latestBatteryPower.getPermissions().contains("discharge_allowed")) {
            homeBatteryPowerMeter.changePermissions(true, true);
        }
    }

}
