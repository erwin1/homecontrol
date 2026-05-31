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

    Uni<Integer> getActivePowerW();

    int getCurrentChargingAmps();

    int getLastKnownActivePower();

    int getChargingMeterReading();

    void changeChargingAmps(int amps, int voltage);

    void stopCharging();

}
