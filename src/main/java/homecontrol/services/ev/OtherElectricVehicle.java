package homecontrol.services.ev;

import io.smallrye.mutiny.Uni;

public class OtherElectricVehicle implements ElectricVehicle {
    @Override
    public String getName() {
        return "Other";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public EVState getCurrentState(StateRefresh stateRefresh) throws EVException {
        return null;
    }

    @Override
    public Uni<Boolean> openChargePortDoor() throws EVException {
        return null;
    }

    @Override
    public void debug() throws EVException {

    }
}
