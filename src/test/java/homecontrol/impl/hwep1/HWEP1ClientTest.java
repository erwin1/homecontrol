package homecontrol.impl.hwep1;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class HWEP1ClientTest {
    @Inject
    private HWEP1Client hwep1Client;

    @Test
    public void test1() {
        Telegram telegram = hwep1Client.parseTelegram(body);
        assertEquals("231104110912", telegram.getTimestamp());
        assertTrue(new BigDecimal("4233.767").equals(telegram.getTotal_power_export_t1_kwh()));
        assertTrue(new BigDecimal("4287.179").equals(telegram.getTotal_power_import_t1_kwh()));
        assertTrue(new BigDecimal("1314.651").equals(telegram.getTotal_power_export_t2_kwh()));
        assertTrue(new BigDecimal("9813.372").equals(telegram.getTotal_power_import_t2_kwh()));
        assertTrue(new BigDecimal("2127.995").equals(telegram.getTotal_gas_m3()));
        assertTrue(new BigDecimal("5548.418").equals(telegram.getTotal_power_export_kwh()));
        assertTrue(new BigDecimal("14100.551").equals(telegram.getTotal_power_import_kwh()), telegram.getTotal_power_import_kwh()+"x");
        assertTrue(new BigDecimal("228.5").equals(telegram.getActive_voltage_v()));
        assertEquals(777, telegram.getActive_power_import_w());
        assertEquals(0, telegram.getActive_power_export_w());
        assertEquals(325, telegram.getActive_power_average_w());
        assertEquals(3018, telegram.getMontly_power_peak_w());
        assertEquals("231101174500", telegram.getMontly_power_peak_timestamp());
    }

    @Test
    public void testDSTTime() {
        Telegram telegram = hwep1Client.parseTelegram(bodyDST);
        assertEquals("240341110912", telegram.getTimestamp());
    }


    String body = "/FLU5\\XXXXXXXX_A\n" +
            "\n" +
            "0-0:96.1.4(50217)\n" +
            "0-0:96.1.1(XXXXXXXXXXXXXXXXXXXXXXXX)\n" +
            "0-0:1.0.0(231104110912W)\n" +
            "1-0:1.8.1(004287.179*kWh)\n" +
            "1-0:1.8.2(009813.372*kWh)\n" +
            "1-0:2.8.1(004233.767*kWh)\n" +
            "1-0:2.8.2(001314.651*kWh)\n" +
            "0-0:96.14.0(0002)\n" +
            "1-0:1.4.0(00.325*kW)\n" +
            "1-0:1.6.0(231101174500W)(03.018*kW)\n" +
            "0-0:98.1.0(11)(1-0:1.6.0)(1-0:1.6.0)(230101000000W)(221203081500W)(06.367*kW)(230201000000W)(230101141500W)(05.475*kW)(230301000000W)(230215083000W)(05.749*kW)(230401000000S)(230305180000W)(04.933*kW)(230501000000S)(230423174500S)(04.055*kW)(230601000000S)(230512180000S)(02.907*kW)(230701000000S)(230625101500S)(02.499*kW)(230801000000S)(230705211500S)(03.194*kW)(230901000000S)(230820181500S)(03.104*kW)(231001000000S)(230923004500S)(02.872*kW)(231101000000W)(231019010000S)(04.166*kW)\n" +
            "1-0:1.7.0(00.777*kW)\n" +
            "1-0:2.7.0(00.000*kW)\n" +
            "1-0:21.7.0(00.777*kW)\n" +
            "1-0:22.7.0(00.000*kW)\n" +
            "1-0:32.7.0(228.5*V)\n" +
            "1-0:31.7.0(003.75*A)\n" +
            "0-0:96.3.10(1)\n" +
            "0-0:17.0.0(999.9*kW)\n" +
            "1-0:31.4.0(999*A)\n" +
            "0-0:96.13.0()\n" +
            "0-1:24.1.0(003)\n" +
            "0-1:96.1.1(37464C4F32313230313033363831)\n" +
            "0-1:24.4.0(1)\n" +
            "0-1:24.2.3(231104110500W)(02127.995*m3)\n" +
            "!XXXX";

    String bodyDST = "/FLU5\\XXXXXXXX_A\n" +
            "\n" +
            "0-0:96.1.4(50217)\n" +
            "0-0:96.1.1(XXXXXXXXXXXXXXXXXXXXXXXX)\n" +
            "0-0:1.0.0(240341110912S)\n" +
            "1-0:1.8.1(004287.179*kWh)\n" +
            "1-0:1.8.2(009813.372*kWh)\n" +
            "1-0:2.8.1(004233.767*kWh)\n" +
            "1-0:2.8.2(001314.651*kWh)\n" +
            "0-0:96.14.0(0002)\n" +
            "1-0:1.4.0(00.325*kW)\n" +
            "1-0:1.6.0(231101174500W)(03.018*kW)\n" +
            "0-0:98.1.0(11)(1-0:1.6.0)(1-0:1.6.0)(230101000000W)(221203081500W)(06.367*kW)(230201000000W)(230101141500W)(05.475*kW)(230301000000W)(230215083000W)(05.749*kW)(230401000000S)(230305180000W)(04.933*kW)(230501000000S)(230423174500S)(04.055*kW)(230601000000S)(230512180000S)(02.907*kW)(230701000000S)(230625101500S)(02.499*kW)(230801000000S)(230705211500S)(03.194*kW)(230901000000S)(230820181500S)(03.104*kW)(231001000000S)(230923004500S)(02.872*kW)(231101000000W)(231019010000S)(04.166*kW)\n" +
            "1-0:1.7.0(00.777*kW)\n" +
            "1-0:2.7.0(00.000*kW)\n" +
            "1-0:21.7.0(00.777*kW)\n" +
            "1-0:22.7.0(00.000*kW)\n" +
            "1-0:32.7.0(228.5*V)\n" +
            "1-0:31.7.0(003.75*A)\n" +
            "0-0:96.3.10(1)\n" +
            "0-0:17.0.0(999.9*kW)\n" +
            "1-0:31.4.0(999*A)\n" +
            "0-0:96.13.0()\n" +
            "0-1:24.1.0(003)\n" +
            "0-1:96.1.1(37464C4F32313230313033363831)\n" +
            "0-1:24.4.0(1)\n" +
            "0-1:24.2.3(231104110500W)(02127.995*m3)\n" +
            "!XXXX";

}

