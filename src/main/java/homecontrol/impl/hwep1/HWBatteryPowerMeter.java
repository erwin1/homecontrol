package homecontrol.impl.hwep1;

import homecontrol.services.powermeter.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class HWBatteryPowerMeter implements HomeBatteryPowerMeter {
    public static final Logger LOGGER = Logger.getLogger(HWBatteryPowerMeter.class.getName());

    @Inject
    private HWEP1Client hwep1Client;

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    @Asynchronous
    public Uni<BatteryMeterReading> getCurrentReading() {
        JsonObject object = null;
        try {
            object = hwep1Client.getBatteryMeasurement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Uni.createFrom().item(new BatteryMeterReading()
                .setCycles(object.getInteger("cycles"))
                .setEnergy_export_kwh(BigDecimal.valueOf(object.getDouble("energy_export_kwh")))
                .setEnergy_import_kwh(BigDecimal.valueOf(object.getDouble("energy_import_kwh")))
                .setState_of_charge_pct(object.getInteger("state_of_charge_pct")));
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    @Asynchronous
    public Uni<BatteryPower> getActivePower() {
        JsonObject object = null;
        try {
            object = hwep1Client.getBatteries();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("object = " + object);
        return Uni.createFrom().item(
                new BatteryPower(
                        object.getInteger("power_w"),
                        object.getString("mode"),
                        object.getJsonArray("permissions").stream().map(Object::toString).toList()
                )
        );
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void changePermissions(boolean dischargeAllow, boolean chargeAllowed) {
        try {
            List<String> permissions = new LinkedList<>();
            if (dischargeAllow) {
                permissions.add("discharge_allowed");
            }
            if (chargeAllowed) {
                permissions.add("charge_allowed");
            }
            LOGGER.info("setting battery permissions: "+permissions);
            hwep1Client.putBatteriesPermissions(permissions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

