package evcharging.services;

public interface EVCharger {
    enum State {
        NotConnected,
        Waiting,
        InProgress
    }

    State getState();
}
