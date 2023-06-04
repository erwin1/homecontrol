package evcharging.impl.homewizard;

import evcharging.services.ElectricityMeter;
import evcharging.services.MeterData;
import evcharging.services.MeterReading;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@LookupIfProperty(name = "evcharging.meter", stringValue = "hwep1", lookupIfMissing = true)
@ApplicationScoped
public class HWEP1 implements ElectricityMeter {
    @ConfigProperty(name = "EVCHARGING_HWEP1_IP")
    String ip;

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public MeterData getLivePowerData() {
        JsonObject responseObject = null;
        try {
            responseObject = getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MeterData meterData = new MeterData(responseObject.getInteger("active_power_w"),
                responseObject.getInteger("active_power_average_w"));

        return meterData;
    }

    @Retry(maxRetries = 10, delay = 3, delayUnit = ChronoUnit.SECONDS)
    public HWMeterData getCurrentHWData() {
        JsonObject responseObject = null;
        try {
            responseObject = getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HWMeterData data = new HWMeterData();

        data.setTotal_power_import_kwh(new BigDecimal(responseObject.getString("total_power_import_kwh")));
        data.setTotal_power_import_t1_kwh(new BigDecimal(responseObject.getString("total_power_import_t1_kwh")));
        data.setTotal_power_import_t2_kwh(new BigDecimal(responseObject.getString("total_power_import_t2_kwh")));
        data.setTotal_power_export_kwh(new BigDecimal(responseObject.getString("total_power_export_kwh")));
        data.setTotal_power_export_t1_kwh(new BigDecimal(responseObject.getString("total_power_export_t1_kwh")));
        data.setTotal_power_export_t2_kwh(new BigDecimal(responseObject.getString("total_power_export_t2_kwh")));
        data.setActive_power_w(new BigDecimal(responseObject.getString("active_power_w")));
        data.setActive_power_average_w(new BigDecimal(responseObject.getString("active_power_average_w")));
        data.setMontly_power_peak_w(new BigDecimal(responseObject.getString("montly_power_peak_w")));
        data.setMontly_power_peak_timestamp(responseObject.getString("montly_power_peak_timestamp"));
        data.setTotal_gas_m3(new BigDecimal(responseObject.getString("total_gas_m3")));

        return data;
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public MeterReading getCurrentMonthPeak() {
        JsonObject responseObject = null;
        try {
            responseObject = getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new MeterReading(LocalDateTime.parse(responseObject.getString("montly_power_peak_timestamp"),
                DateTimeFormatter.ofPattern("yyMMddHHmmss")).atZone(ZoneId.of("Europe/Brussels")),
                responseObject.getInteger("montly_power_peak_w"));
    }

    private JsonObject getData() throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+ ip + "/api/v1/data"))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error "+response.statusCode());
        }
        JsonObject object = new JsonObject(response.body());
        return object;
    }


}
