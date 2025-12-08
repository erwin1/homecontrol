package homecontrol.services.powercontrol;

import homecontrol.metrics.MetricsLogger;
import homecontrol.services.config.*;
import homecontrol.services.ev.Charger;
import homecontrol.services.ev.EVException;
import homecontrol.services.ev.EVState;
import homecontrol.services.ev.ElectricVehicle;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powercontrol.PowerControlService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.solar.Inverter;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@QuarkusTest
public class PowerControlServiceTest {

    @InjectMock
    private ElectricalPowerMeter electricalPowerMeter;
    @InjectMock
    private Inverter inverter;
    @InjectMock
    private Charger charger;
    @InjectMock
    private ConfigService configService;
    @InjectMock
    private ElectricVehicle electricVehicle;
    @InjectMock
    private NotificationService notificationService;
    @InjectMock
    private MetricsLogger metricsLogger;

    @Inject
    private EVControlService evControlService;

    @Inject
    private PowerControlService powerControlService;

    @BeforeEach
    public void beforeEach() {
        Mockito.clearInvocations();
    }

    @Test
    public void testExp1() throws EVException {
        presetState(TimeType.PEAK, 3000, 1000, 1500, 0, 50);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(9);
    }

    @Test
    public void testExp2() throws EVException {
        presetState(TimeType.PEAK, 2100, 800, 1700, 0, 50);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(6);
    }

    @Test
    public void testExp3() throws EVException {
        presetState(TimeType.PEAK, 2000, 1100, 1500, 0, 50);
        powerControlService.controlEVCharging();
        checkChargingStopped();
    }

    @Test
    public void testExp4() throws EVException {
        presetState(TimeType.PEAK, 4000, 500, 0, 0, 50);
        powerControlService.controlEVCharging();
        checkChargingStartedAt(16);
    }

    @Test
    public void testExp5() throws EVException {
        presetState(TimeType.PEAK, 1000, 500, 0, 0, 50);
        powerControlService.controlEVCharging();
        checkNoChange();
    }

    @Test
    public void testExp6() throws EVException {
        presetState(TimeType.PEAK, 5000, 500, 0, 0, 90);
        powerControlService.controlEVCharging();
        checkNoChange();
    }

    @Test
    public void testExp7() throws EVException {
        presetState(TimeType.OFF, 500, 500, 1000, 0, 70);
        powerControlService.controlEVCharging();
        checkChargingStopped();
    }

    @Test
    public void testMax1() throws EVException {
        presetState(TimeType.OFF, 1000, 500, 1100, 0, 50);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(20);
    }

    @Test
    public void testMax2() throws EVException {
        presetState(TimeType.OFF, 4800, 800, 0, 0, 50);
        powerControlService.controlEVCharging();
        checkChargingStartedAt(36);
    }

    @Test
    public void testMax3() throws EVException {
        presetState(TimeType.OFF, 2000, 500, 2000, 0, 50);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(25);
    }

    @Test
    public void testMax4() throws EVException {
        presetState(ZonedDateTime.parse("2023-10-28T11:10:00+02:00"), 1000, 500, 2000, 3000, 50, 4000);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(15);
    }

    @Test
    public void testMax5() throws EVException {
        presetState(ZonedDateTime.parse("2023-10-28T11:10:00+02:00"), 1000, 500, 2000, 3700, 50, 4000);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(6);
    }

    @Test
    public void testMax6() throws EVException {
        presetState(ZonedDateTime.parse("2023-10-28T11:10:00+02:00"), 900, 500, 2000, 3800, 50, 4000);
        powerControlService.controlEVCharging();
        checkChargingStopped();
    }

    @Test
    public void testMax7() throws EVException {
        presetState(TimeType.OFF, 1000, 500, 0, 0, 50);
        powerControlService.controlEVCharging();
        checkChargingStartedAt(20);

        int avg = (4500-1000+500)/60*4;
        System.out.println("avg = " + avg);

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:01:00+02:00"), 1000, 500, 4500, avg, 50, 4000);
        powerControlService.controlEVCharging();
        checkNoChange();

        avg+=(4500-1000+500)/60*4;

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:02:00+02:00"), 1000, 500, 4500, avg, 50, 4000);
        powerControlService.controlEVCharging();
        checkNoChange();

        avg+=(4500-1000+500)/60*4;

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:03:00+02:00"), 1000, 2000, 4500, avg, 50, 4000);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(13);

        avg+=(2860-1000+2000)/60*4;

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:04:00+02:00"), 1000, 2000, 2860, avg, 50, 4000);
        powerControlService.controlEVCharging();
        checkNoChange();

        avg+=(2860-1000+2000)/60*4;

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:05:00+02:00"), 5000, 1500, 2860, avg, 50, 4000);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(34);
    }

    @Test
    public void testMax8() throws EVException {
        presetState(ZonedDateTime.parse("2023-10-28T11:00:47+02:00"), 0, 410, 3026, 183, 50, 3380, 228);
        powerControlService.controlEVCharging();
        checkNoChange();
        Mockito.clearInvocations(charger);

//        Mockito.clearInvocations(electricVehicle);
        presetState(ZonedDateTime.parse("2023-10-28T11:01:47+02:00"), 0, 412, 3042, 413, 50, 3380, 228);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(12);
//        Mockito.clearInvocations(electricVehicle);

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:11:47+02:00"), 0, 234, 3020, 2667, 50, 3380, 228);
        powerControlService.controlEVCharging();
        checkNoChange();
//        Mockito.clearInvocations(electricVehicle);

        Mockito.clearInvocations(charger);
        presetState(ZonedDateTime.parse("2023-10-28T11:13:47+02:00"), 0, 168, 3259, 3121, 50, 3380, 228);
        powerControlService.controlEVCharging();
        checkKeepsChargingAt(13);
//        Mockito.clearInvocations(electricVehicle);
    }

    public void checkChargingStartedAt(int amps) throws EVException {
//        Mockito.verify(charger).startCharging();
        Mockito.verify(charger, Mockito.never()).stopCharging();
        Mockito.verify(charger).changeChargingAmps(Mockito.eq(amps));
    }

    public void checkChargingStopped() throws EVException {
        Mockito.verify(charger).stopCharging();
    }

    public void checkNoChange() throws EVException {
        Mockito.verify(charger, Mockito.never()).stopCharging();
        Mockito.verify(charger, Mockito.never()).changeChargingAmps(Mockito.anyInt());
    }

    public void checkKeepsChargingAt(int amps) throws EVException {
        Mockito.verify(charger, Mockito.never()).stopCharging();
        Mockito.verify(charger).changeChargingAmps(Mockito.eq(amps));
    }

    private void presetState(TimeType timeType, int solarYield, int powerUsageWithoutEV, int powerUsageEVCharging, int activePowerAverage, int currentBatteryLevel) throws EVException {
        ZonedDateTime ts;
        if (timeType.equals(TimeType.OFF)) {
            ts = ZonedDateTime.parse("2023-10-28T11:00:00+02:00");
        } else {
            ts = ZonedDateTime.parse("2023-10-23T12:00:00+02:00");
        }
        presetState(ts, solarYield, powerUsageWithoutEV, powerUsageEVCharging, activePowerAverage, currentBatteryLevel, 4000);
    }

    private void presetState(ZonedDateTime ts, int solarYield, int powerUsageWithoutEV, int powerUsageEVCharging, int activePowerAverage, int currentBatteryLevel, int maxPeak) throws EVException {
        presetState(ts, solarYield, powerUsageWithoutEV, powerUsageEVCharging, activePowerAverage, currentBatteryLevel, maxPeak, 220);
    }

    private void presetState(ZonedDateTime ts, int solarYield, int powerUsageWithoutEV, int powerUsageEVCharging, int activePowerAverage, int currentBatteryLevel, int maxPeak, int activeVoltage) throws EVException {
        QuarkusMock.installMockForType(Clock.fixed(ts.toInstant(), ZoneId.systemDefault()), Clock.class);
        Mockito.when(charger.getCurrentState(Mockito.any())).thenReturn(powerUsageEVCharging > 0 ? Charger.State.InProgress : Charger.State.Waiting);
        Mockito.when(configService.getPeakStrategy()).thenReturn(PeakStrategy.STATIC_LIMITED);
        Mockito.when(configService.getMax15minPeak()).thenReturn(maxPeak);
        Mockito.when(configService.getEVChargingConfigs()).thenReturn(powerConfig());
        Mockito.when(configService.getMinimumChargingA()).thenReturn(6);
        Mockito.when(configService.getCurrentMode()).thenReturn(Mode.ON);

        int activePower = powerUsageEVCharging + powerUsageWithoutEV - solarYield;
        EVState evState = new EVState();
        evState.setCharge_limit_soc_max(90);
        evState.setCharge_limit_soc(90);
        evState.setCharge_current_request_max(32);
        evState.setCharge_amps(10);
        evState.setCharging_state(powerUsageEVCharging > 0 ? "Charging" : "Stopped");
        evState.setBattery_level(currentBatteryLevel);
        evState.setCharge_amps(powerUsageEVCharging / activeVoltage);
        System.out.println("evState = " + evState.getCharge_amps());

        Mockito.when(electricVehicle.getCurrentState(Mockito.any())).thenReturn(evState);
        Mockito.when(electricVehicle.isVehicleOnline()).thenReturn(true);

        if (powerUsageEVCharging > 1380) {
            evControlService.changeCharging(powerUsageEVCharging / 220);
        }
        Mockito.clearInvocations(charger);

        Mockito.when(electricalPowerMeter.getActivePower()).thenReturn(Uni.createFrom().item(new ActivePower(ts, activePower, activePowerAverage, new BigDecimal(activeVoltage))));
        Mockito.when(inverter.getCurrentYield()).thenReturn(Uni.createFrom().item(solarYield));
        Mockito.when(charger.getActivePower()).thenReturn(Uni.createFrom().item(powerUsageEVCharging));
    }

    private EVChargingConfigs powerConfig() {
        EVChargingConfigs evChargingConfig = new EVChargingConfigs();

        EVChargingStrategy typeExp = new EVChargingStrategy(EVChargingStrategy.Type.EXP);
        EVChargingStrategy typeMax = new EVChargingStrategy(EVChargingStrategy.Type.MAX);
        EVChargingStrategy typeMin = new EVChargingStrategy(EVChargingStrategy.Type.EXP_PLUS, 100, 500);


        evChargingConfig.addConfig(60, TimeType.PEAK, typeExp);
        evChargingConfig.addConfig(60, TimeType.OFF, typeMax);

        evChargingConfig.addConfig(80, TimeType.PEAK, typeExp);
        evChargingConfig.addConfig(80, TimeType.OFF, typeMin);

        return evChargingConfig;
    }

}
