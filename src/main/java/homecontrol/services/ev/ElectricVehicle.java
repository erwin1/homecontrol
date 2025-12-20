package homecontrol.services.ev;

import io.smallrye.mutiny.Uni;

public interface ElectricVehicle {

    String getName();

    boolean isConfigured();

    EVState getCurrentState(StateRefresh stateRefresh) throws EVException;

    Uni<Boolean> openChargePortDoor() throws EVException;

    void debug() throws EVException;
}
