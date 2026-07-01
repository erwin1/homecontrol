package homecontrol.services.powercontrol;

import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.ev.EVException;
import homecontrol.services.powermeter.ActivePower;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class PowerCalculatorTest {

    @Inject
    private OptimalExportChargingService optimalExportChargingService;
    
    @Inject
    PowerCalculator powerCalculator;
    EVChargingStrategy exp = new EVChargingStrategy(EVChargingStrategy.Type.EXP);
    EVChargingStrategy max = new EVChargingStrategy(EVChargingStrategy.Type.MAX);
    EVChargingStrategy maxOpt = new EVChargingStrategy(EVChargingStrategy.Type.MAX_OPT);
    EVChargingStrategy expOpt1 = new EVChargingStrategy(EVChargingStrategy.Type.EXP_OPT1, 100, 500, 2);
    EVChargingStrategy expOpt2 = new EVChargingStrategy(EVChargingStrategy.Type.EXP_OPT2, 100, 1000, 2);

    int defaultMonthlyPeak = 2500;
    int minimumChargingA = 5;

    @BeforeEach
    public void beforeEach() {
        optimalExportChargingService.reset();
    }

    @Test
    public void testOptExp2a() throws EVException {
        optimalExportChargingService.reset();
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:05", -500, 0), 0, 100, defaultMonthlyPeak, 3000, 1500,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:06", 1500, 0), 0, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:07", 1500, 0), 0, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:08", 600, 0), 0, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(0, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:09", 600, 0), 0, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testOptExp2b() throws EVException {
        optimalExportChargingService.reset();
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:05", -500, 0), 0, 100, defaultMonthlyPeak, 3000, 1500,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:06", 1500, 0), 0, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:07", 1500, 0), 0, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:08", 600, 0), 0, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(14, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:09", -2000, 0), 0, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testOptExp2c() throws EVException {
        optimalExportChargingService.reset();
        assertEquals(6, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:05", -500+800, 0), 800, 100, defaultMonthlyPeak, 3000, 1500,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:06", 1500+800, 0), 800, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:07", 1500-800, 0), -800, 100, defaultMonthlyPeak, 1000, 2000,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:08", 600-800, 0), -800, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(14, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:09", -2000-800, 0), -800, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:10", 100, 0), -800, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt2, activePower("05:05:11", 100, 0), -800, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
        assertEquals(5, powerCalculator.calculateOptimalChargingA(//TODO 0!?!?!?!
                expOpt2, activePower("05:05:12", 100, 0), -800, 100, defaultMonthlyPeak, 1000, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }


    @Test
    public void testExp1() throws EVException {
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                exp, activePower("05:05:05", -500, 0), 0, 100, defaultMonthlyPeak, 3000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExp2() throws EVException {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                exp, activePower("05:05:05", 1000, 0), 0, 100, defaultMonthlyPeak, 2000, 2200,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExp3() throws EVException {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                exp, activePower("05:05:05", 500, 0), 0, 100, defaultMonthlyPeak, 2000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExp3b() throws EVException {
        assertEquals(0, powerCalculator.calculateOptimalChargingA(
                exp, activePower("05:05:05", 600, 0), 0, 100, defaultMonthlyPeak, 2000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExp4() throws EVException {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                exp, activePower("05:05:05", -1200, 0), 0, 100, defaultMonthlyPeak, 2000, 0, minimumChargingA,  optimalExportChargingService
        ));
    }

    @Test
    public void testExpOpt1() throws EVException {
        optimalExportChargingService.reset();
        assertEquals(8, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("05:05:05", -450, 0), 800, 100, defaultMonthlyPeak, 3000, 1400, minimumChargingA,  optimalExportChargingService
        ));
    }

    @Test
    public void testExpOpt2() throws EVException {
        optimalExportChargingService.reset();
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("05:05:05", 0, 0), -450, 50, defaultMonthlyPeak, 3000, 1850, minimumChargingA,  optimalExportChargingService
        ));
    }

    @Test
    public void testMax1() {
        assertEquals(12, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:01:00", 0, 0), 0, 100, defaultMonthlyPeak, 0, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax2() {
        assertEquals(16, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:01:00", -1000, 0), 0, 100, defaultMonthlyPeak, 1000, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax3() {
        assertEquals(16, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:01:00", -1000, 0), 0, 100, defaultMonthlyPeak, 2000, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax4() {
        assertEquals(11, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", 0, 1250), 0, 100, defaultMonthlyPeak, 0, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax5() {
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", 500, 1250), 0, 100, defaultMonthlyPeak, 0, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax6() {
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", 500, 1250), 0, 100, defaultMonthlyPeak, 1000, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax7() {
        assertEquals(15, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", 500, 1250), 0, 100, defaultMonthlyPeak, 1000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax8() {
        assertEquals(18, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", 500, 1250), 0, 100, defaultMonthlyPeak, 2000, 2000,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax9() {
        assertEquals(20, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:07:30", -500, 1250), 0, 100, defaultMonthlyPeak, 2000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax10() {
        assertEquals(29, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:10:00", -500, 1000), 0, 100, defaultMonthlyPeak, 2000, 1500,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax11() {
        assertEquals(22, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:13:30", 1200, 2000), 0, 100, defaultMonthlyPeak, 0, 1200,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMax12() {
        assertEquals(6, powerCalculator.calculateOptimalChargingA(
                max, activePower("00:10:00", 1200, 2000), 0, 100, defaultMonthlyPeak, 0, 1200,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt1() {
        assertEquals(6, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:00:30", 1500, 100), 0, 100, defaultMonthlyPeak, 0, 1380,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt2() {
        assertEquals(10, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:01:30", 1500, 150), 0, 100, defaultMonthlyPeak, 0, 1380,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt3() {
        assertEquals(11, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:03:00", 2600, 350), 0, 100, defaultMonthlyPeak, 0, 2420,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt4() {
        assertEquals(9, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:03:00", 2600, 600), 0, 100, defaultMonthlyPeak, 0, 2420,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt5() {
        assertEquals(11, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:03:00", 2600, 150), 0, 100, defaultMonthlyPeak, 0, 2420,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt6() {
        assertEquals(11, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:03:00", 300, 150), 0, 100, defaultMonthlyPeak, 0, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testMaxOpt7() {
        assertEquals(15, powerCalculator.calculateOptimalChargingA(
                maxOpt, activePower("00:03:00", 1600, 350), 0, 100, defaultMonthlyPeak, 1000, 2420,  minimumChargingA, optimalExportChargingService
        ));
    }



    @Test
    public void testExpPlus1() {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:00:00", -600, 0), 0, 100, defaultMonthlyPeak, 800, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus2() {
        assertEquals(6, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:00:00", -200, 0), 0, 100, defaultMonthlyPeak, 1500, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus3() {
        assertEquals(0, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:10:00",2000, 2000), 0, 100, defaultMonthlyPeak, 800, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus4() {
        assertEquals(0, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:10:00", 1000, 2000), 0, 100, defaultMonthlyPeak, 1800, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus5() {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:10:00", 500, 1965), 0, 100, defaultMonthlyPeak, 1800, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus6() {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:10:00",500, 2400), 0, 100, defaultMonthlyPeak, 1800, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus7() {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:05:00",500, 1500), 0, 100, defaultMonthlyPeak, 1800, 1100,  minimumChargingA, optimalExportChargingService
        ));
    }

    @Test
    public void testExpPlus8() {
        assertEquals(5, powerCalculator.calculateOptimalChargingA(
                expOpt1, activePower("00:10:00",-500, 2000), 0, 100, defaultMonthlyPeak, 1000, 0,  minimumChargingA, optimalExportChargingService
        ));
    }

    private ActivePower activePower(String time, int power, int avg) {
        return new ActivePower(ZonedDateTime.parse("2023-10-23T"+time+"+02:00"), power, avg, new BigDecimal(220));
    }
}
