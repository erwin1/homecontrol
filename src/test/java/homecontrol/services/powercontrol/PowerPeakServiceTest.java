package homecontrol.services.powercontrol;

import homecontrol.services.config.ConfigService;
import homecontrol.services.config.PeakStrategy;
import homecontrol.services.ev.Charger;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.powermeter.MonthlyPowerPeak;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PowerPeakServiceTest {
    @InjectMock
    private ConfigService configService;
    @InjectMock
    private ElectricalPowerMeter electricalPowerMeter;
    @InjectMock
    private Charger charger;
    @InjectMock
    private NotificationService notificationService;

    @Inject
    PowerPeakService powerPeakService;

    @Test
    public void testCurrentMonthPeak1() {
        presetCurrentMonthPeak(3000, PeakStrategy.DYNAMIC_UNLIMITED);
        assertEquals(3000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak2() {
        presetCurrentMonthPeak(2400, PeakStrategy.DYNAMIC_UNLIMITED);
        assertEquals(2500, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak3() {
        presetCurrentMonthPeak(4100, PeakStrategy.DYNAMIC_UNLIMITED);
        assertEquals(4100, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak4() {
        presetCurrentMonthPeak(3000, PeakStrategy.DYNAMIC_LIMITED);
        assertEquals(3000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak5() {
        presetCurrentMonthPeak(2400, PeakStrategy.DYNAMIC_LIMITED);
        assertEquals(2500, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak6() {
        presetCurrentMonthPeak(4100, PeakStrategy.DYNAMIC_LIMITED);
        assertEquals(4000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak7() {
        presetCurrentMonthPeak(3000, PeakStrategy.STATIC_LIMITED);
        assertEquals(4000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak8() {
        presetCurrentMonthPeak(2400, PeakStrategy.STATIC_LIMITED);
        assertEquals(4000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeak9() {
        presetCurrentMonthPeak(4100, PeakStrategy.STATIC_LIMITED);
        assertEquals(4000, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testCurrentMonthPeakNotAvailable() {
        when(configService.getPeakStrategy()).thenReturn(PeakStrategy.DYNAMIC_UNLIMITED);
        when(configService.getMax15minPeak()).thenReturn(4000);
        when(configService.getMin15minPeak()).thenReturn(2500);
        when(electricalPowerMeter.getMonthlyPowerPeak()).thenThrow(new RuntimeException("unexpected error"));

        assertEquals(2500, powerPeakService.getCurrentMonth15minPeak());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod1() {
        mockEstimate("00:00:00", 2000, 0, 0);
        assertEquals(2000, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod2() {
        mockEstimate("00:01:00", 2000, 100, 0);
        assertEquals(1500, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod3() {
        mockEstimate("00:10:00", 5000, 1000, 0);
        assertEquals(1500, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod4() {
        mockEstimate("00:00:00", 2000, 0, 1000);
        assertEquals(1000, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod5() {
        mockEstimate("00:01:00", 2000, 100, 1000);
        assertEquals(568, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }

    @Test
    public void testEstimatePeakInCurrentPeriod6() {
        mockEstimate("00:10:00", 5000, 1000, 1000);
        assertEquals(1168, powerPeakService.estimatePeakInCurrentPeriod().getItem1());
    }


    private void presetCurrentMonthPeak(int monthPeak, PeakStrategy peakStrategy) {
        QuarkusMock.installMockForType(Clock.fixed(Instant.parse("2023-10-30T00:00:00+01:00"), ZoneId.systemDefault()), Clock.class);
        when(electricalPowerMeter.getMonthlyPowerPeak()).thenReturn(
                new MonthlyPowerPeak(ZonedDateTime.parse("2023-10-15T12:00:00+02:00"), monthPeak));
        when(configService.getPeakStrategy()).thenReturn(peakStrategy);
        when(configService.getMax15minPeak()).thenReturn(4000);
        when(configService.getMin15minPeak()).thenReturn(2500);

    }

    private void mockEstimate(String time, int activePower, int activePowerAvg, int charging) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2023-10-28T"+time+"+02:00");

        QuarkusMock.installMockForType(Clock.fixed(Instant.parse("2023-10-23T"+time+"+02:00"), ZoneId.systemDefault()), Clock.class);
        if (charging == 0) {
            when(charger.getCurrentState(Mockito.any())).thenReturn(Charger.State.NotConnected);
        } else {
            when(charger.getCurrentState(Mockito.any())).thenReturn(Charger.State.InProgress);
            when(charger.getActivePower()).thenReturn(Uni.createFrom().item(charging));
        }
        when(electricalPowerMeter.getActivePower()).thenReturn(Uni.createFrom().item(new ActivePower(zonedDateTime, activePower, activePowerAvg, new BigDecimal(228.2))));
    }
}
