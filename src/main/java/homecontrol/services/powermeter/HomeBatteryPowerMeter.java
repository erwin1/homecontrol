package homecontrol.services.powermeter;

import io.smallrye.mutiny.Uni;

public interface HomeBatteryPowerMeter {

    Uni<BatteryMeterReading> getCurrentReading();

    Uni<BatteryPower> getActivePower();

    void changePermissions(boolean dischargeAllow, boolean chargeAllowed);

}
