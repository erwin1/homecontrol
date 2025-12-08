package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.config.ConfigService;
import homecontrol.services.ev.Charger;
import homecontrol.services.ev.EVException;
import homecontrol.services.ev.EVState;
import homecontrol.services.ev.ElectricVehicle;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powercontrol.EVControlService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

@QuarkusTest
public class EVControlServiceTest {
    @InjectMock
    private ElectricVehicle electricVehicle;
    @InjectMock
    private NotificationService notificationService;
    @InjectMock
    private MetricsLogger metricsLogger;
    @InjectMock
    private ConfigService configService;
    @InjectMock
    private Charger charger;

    @Inject
    private EVControlService evControlService;

    @Test
    public void testChangeCharging1() throws EVException {
        preset(50, 0);
        evControlService.changeCharging(9);
        verify(charger).changeChargingAmps(9);
    }

    @Test
    public void testChangeCharging2() throws EVException {
        preset(50, 2000);
        evControlService.changeCharging(10);
        verify(charger).changeChargingAmps(10);
    }

    @Test
    public void testChangeCharging3() throws EVException {
        preset(50, 2000);
        evControlService.changeCharging(0);
        verify(charger).stopCharging();
    }

    @Test
    public void testChangeCharging4() throws EVException {
        preset(50, 1380);
        evControlService.changeCharging(6);
        verify(charger, never()).stopCharging();
        verify(charger).changeChargingAmps(6);
    }

    @Test
    public void testChangeCharging5() throws EVException {
        preset(50, 1100);
        evControlService.changeCharging(32);
        verify(charger, never()).stopCharging();
        verify(charger).changeChargingAmps(32);
    }

    private void preset(int batteryLevel, int power) throws EVException {
        if (power > 0) {
            evControlService.handleChargerState(Charger.State.InProgress);
        } else {
            evControlService.handleChargerState(Charger.State.Waiting);
        }
        when(configService.getMinimumChargingA()).thenReturn(6);
    }

}
