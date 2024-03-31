package homecontrol.services.ev;

import io.smallrye.mutiny.Uni;

public interface Charger {
    enum State {
        NotConnected,
        Initializing,
        Waiting,
        InProgress
    }

    State getCurrentState(StateRefresh stateRefresh);

    Uni<Integer> getActivePower();

    int getChargingMeterReading();

}
