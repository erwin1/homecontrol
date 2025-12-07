package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class EVControlServiceTest {
    @InjectMock
    private ElectricVehicle electricVehicle;
    @InjectMock
    private NotificationService notificationService;
    @InjectMock
    private MetricsLogger metricsLogger;

    @Inject
    private EVControlService evControlService;

    @Test
    public void testChangeCharging1() throws EVException {
        preset(50, 0);
        evControlService.changeCharging(9);
        verify(electricVehicle).startCharging();
        verify(electricVehicle).changeChargingAmps(9);
    }

    @Test
    public void testChangeCharging2() throws EVException {
        preset(50, 2000);
        evControlService.changeCharging(10);
        verify(electricVehicle, never()).startCharging();
        verify(electricVehicle).changeChargingAmps(10);
    }

    @Test
    public void testChangeCharging3() throws EVException {
        preset(50, 2000);
        evControlService.changeCharging(0);
        verify(electricVehicle).stopCharging(5);
    }

    @Test
    public void testChangeCharging4() throws EVException {
        preset(50, 1100);
        evControlService.changeCharging(5);
        verify(electricVehicle, never()).stopCharging(5);
        verify(electricVehicle, never()).changeChargingAmps(Mockito.anyInt());
    }

    @Test
    public void testChangeCharging5() throws EVException {
        preset(50, 1100);
        evControlService.changeCharging(36);
        verify(electricVehicle, never()).stopCharging(5);
        verify(electricVehicle).changeChargingAmps(32);
    }

    private void preset(int batteryLevel, int power) throws EVException {
        EVState evState = new EVState();
        evState.setCharge_limit_soc_max(90);
        evState.setCharge_limit_soc(90);
        evState.setCharge_current_request_max(32);
        evState.setCharge_amps(10);
        evState.setCharging_state(power > 0 ? "Charging" : "Stopped");
        evState.setBattery_level(batteryLevel);
        evState.setCharge_amps(power / 220);

        Mockito.when(electricVehicle.getCurrentState(Mockito.any())).thenReturn(evState);
        Mockito.when(electricVehicle.isVehicleOnline()).thenReturn(true);

    }

}
