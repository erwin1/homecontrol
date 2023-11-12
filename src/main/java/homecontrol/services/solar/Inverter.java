package homecontrol.services.solar;

import io.smallrye.mutiny.Uni;

public interface Inverter {

    Uni<Integer> getCurrentYield();

    int getYieldMeterReading();

}
