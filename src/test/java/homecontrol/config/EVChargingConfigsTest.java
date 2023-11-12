package homecontrol.config;

import homecontrol.services.config.EVChargingConfigs;
import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.config.TimeType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class EVChargingConfigsTest {

    private EVChargingConfigs configs;

    @BeforeEach
    public void setup() {
        configs = new EVChargingConfigs();
        EVChargingStrategy typeExp = new EVChargingStrategy(EVChargingStrategy.Type.EXP);
        EVChargingStrategy typeMax = new EVChargingStrategy(EVChargingStrategy.Type.MAX);
        EVChargingStrategy typeMin = new EVChargingStrategy(EVChargingStrategy.Type.EXP_PLUS, 100, 500);

        configs.addConfig(50, TimeType.PEAK, typeExp);
        configs.addConfig(50, TimeType.OFF, typeMax);

        configs.addConfig(75, TimeType.PEAK, typeExp);
        configs.addConfig(75, TimeType.OFF, typeMin);
    }

    @Test
    public void test1() {
        assertEquals(EVChargingStrategy.Type.MAX, configs.getEVChargingStrategy(40, TimeType.OFF).getType());
    }

    @Test
    public void test2() {
        assertEquals(EVChargingStrategy.Type.EXP_PLUS, configs.getEVChargingStrategy(50, TimeType.OFF).getType());
        assertEquals(500, configs.getEVChargingStrategy(50, TimeType.OFF).getMinPowerExportW());
        assertEquals(100, configs.getEVChargingStrategy(50, TimeType.OFF).getMinSolarYieldW());
    }

    @Test
    public void test3() {
        assertEquals(EVChargingStrategy.Type.EXP_PLUS, configs.getEVChargingStrategy(74, TimeType.OFF).getType());
        assertEquals(500, configs.getEVChargingStrategy(50, TimeType.OFF).getMinPowerExportW());
        assertEquals(100, configs.getEVChargingStrategy(50, TimeType.OFF).getMinSolarYieldW());
    }

    @Test
    public void test4() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(75, TimeType.OFF).getType());
    }

    @Test
    public void test5() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(99, TimeType.OFF).getType());
    }

    @Test
    public void test6() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(100, TimeType.OFF).getType());
    }

    @Test
    public void test7() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(40, TimeType.PEAK).getType());
    }

    @Test
    public void test8() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(50, TimeType.PEAK).getType());
    }

    @Test
    public void test9() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(74, TimeType.PEAK).getType());
    }

    @Test
    public void test10() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(75, TimeType.PEAK).getType());
    }

    @Test
    public void test11() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(99, TimeType.PEAK).getType());
    }

    @Test
    public void test12() {
        assertEquals(EVChargingStrategy.Type.EXP, configs.getEVChargingStrategy(100, TimeType.PEAK).getType());
    }

}
