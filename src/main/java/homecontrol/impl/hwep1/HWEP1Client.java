package homecontrol.impl.hwep1;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class HWEP1Client {
    public static final Logger LOGGER = Logger.getLogger(HWEP1Client.class.getName());
    private static Pattern patternId = Pattern.compile("(.*?)\\(.*");
    private static Pattern patternValue = Pattern.compile("\\((.*?)\\)");

    @ConfigProperty(name = "EVCHARGING_HWEP1_IP")
    String ip;
    
    @ConfigProperty(name = "HAS_GAS", defaultValue = "true")
    String hasGas;
    

    boolean hasGas() {
        return Boolean.valueOf(hasGas);
    }

    @CacheResult(cacheName = "hwep1-data")
    JsonObject getJsonData() throws IOException {
        LOGGER.fine("Requesting data from HWEP1");

        HttpURLConnection conn = (HttpURLConnection) new URL("http://"+ip+"/api/v1/data").openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("error getting hwep1 data "+conn.getResponseCode());
        }
        String body = new String(conn.getInputStream().readAllBytes());
        conn.getInputStream().close();
        JsonObject object = new JsonObject(body);

        return object;
    }

    @CacheResult(cacheName = "hwep1-telegram")
    public Telegram getTelegram() throws IOException {
        LOGGER.fine("Requesting data from HWEP1");

        HttpURLConnection conn = (HttpURLConnection) new URL("http://"+ip+"/api/v1/telegram").openConnection();
        conn.setRequestProperty("Accept", "text/plain");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("error getting hwep1 data "+conn.getResponseCode());
        }
        String body = new String(conn.getInputStream().readAllBytes());
        conn.getInputStream().close();

        return parseTelegram(body);
    }

    Telegram parseTelegram(String body) {
        Map<String, List<String>> map = body.lines().map(l -> {
            String id = null;
            List<String> values = new LinkedList<>();
            Matcher matcherId = patternId.matcher(l);
            if (matcherId.find()) {
                id = matcherId.group(1);
            }
            Matcher matcher = patternValue.matcher(l);
            while (matcher.find()) {
                String v = matcher.group(1);
                values.add(v);
            }
            return  Tuple2.of(id, values);
        })
        .filter(r -> r.getItem1() != null)
        .collect(Collectors.toMap(r -> r.getItem1(), r -> r.getItem2()));

        Telegram telegram = new Telegram();
        telegram.setTimestamp(map.get("0-0:1.0.0").get(0).replaceAll("W", ""));
        telegram.setActive_voltage_v(new BigDecimal(parseValue(map.get("1-0:32.7.0").get(0))));
        telegram.setTotal_power_import_t1_kwh(new BigDecimal(parseValue(map.get("1-0:1.8.1").get(0))));
        telegram.setTotal_power_import_t2_kwh(new BigDecimal(parseValue(map.get("1-0:1.8.2").get(0))));
        telegram.setTotal_power_export_t1_kwh(new BigDecimal(parseValue(map.get("1-0:2.8.1").get(0))));
        telegram.setTotal_power_export_t2_kwh(new BigDecimal(parseValue(map.get("1-0:2.8.2").get(0))));
        telegram.setTotal_power_import_kwh(telegram.getTotal_power_import_t1_kwh().add(telegram.getTotal_power_import_t2_kwh()));
        telegram.setTotal_power_export_kwh(telegram.getTotal_power_export_t1_kwh().add(telegram.getTotal_power_export_t2_kwh()));
        List<String> gas = map.get("0-1:24.2.3");
        if (gas != null && gas.size() > 1) {
            telegram.setTotal_gas_m3(new BigDecimal(parseValue(gas.get(1))));
        }
        if (hasGas()) telegram.setTotal_gas_m3(new BigDecimal(parseValue(map.get("0-1:24.2.3").get(1))));
        telegram.setActive_power_average_w(new BigDecimal(parseValue(map.get("1-0:1.4.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setActive_power_import_w(new BigDecimal(parseValue(map.get("1-0:1.7.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setActive_power_export_w(new BigDecimal(parseValue(map.get("1-0:2.7.0").get(0))).multiply(new BigDecimal(1000)).intValue());
        telegram.setMontly_power_peak_timestamp(map.get("1-0:1.6.0").get(0).replaceAll("W", ""));
        telegram.setMontly_power_peak_w(new BigDecimal(parseValue(map.get("1-0:1.6.0").get(1))).multiply(new BigDecimal(1000)).intValue());

        return telegram;
    }

    private String parseValue(String v) {
        int x = v.indexOf("*");
        if (x > 0) {
            return v.substring(0, x);
        }
        return v;
    }

}
