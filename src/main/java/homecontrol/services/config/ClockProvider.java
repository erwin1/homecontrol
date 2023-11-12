package homecontrol.services.config;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
import jakarta.ws.rs.Produces;

import java.time.Clock;

public class ClockProvider {

    @Produces
    @Default
    @RequestScoped
    public Clock produces() {
        return Clock.systemDefaultZone();
    }
}
