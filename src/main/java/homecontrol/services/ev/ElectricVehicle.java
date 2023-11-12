package homecontrol.services.ev;

import io.smallrye.mutiny.Uni;

public interface ElectricVehicle {

    EVState getCurrentState(StateRefresh stateRefresh) throws EVException;

    boolean isVehicleOnline() throws EVException;

    void startCharging() throws EVException;

    void stopCharging() throws EVException;

    void changeChargingAmps(int amps) throws EVException;

    void enableScheduledCharging() throws EVException;

    void disableScheduledCharging() throws EVException;

    Uni<Boolean> openChargePortDoor() throws EVException;
}
