package homecontrol.services.ev;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface ElectricVehicle {

    String getName();

    boolean isConfigured();

    EVState getCurrentState(StateRefresh stateRefresh) throws EVException;

    default Optional<EVState> getOdoMeter() throws EVException {
        return Optional.empty();
    }

    Uni<Boolean> openChargePortDoor() throws EVException;

    void debug() throws EVException;
}
