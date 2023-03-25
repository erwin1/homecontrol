package evcharging.impl.homewizard;

import evcharging.services.ElectricityMeter;
import evcharging.services.MeterData;
import evcharging.services.MeterReading;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@LookupIfProperty(name = "evcharging.meter", stringValue = "hwep1", lookupIfMissing = true)
@ApplicationScoped
public class HWEP1 implements ElectricityMeter {
    @ConfigProperty(name = "EVCHARGING_HWEP1_IP")
    String ip;

    @Override
    public MeterData getCurrentData() {
        JsonObject responseObject = null;
        try {
            responseObject = getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MeterData meterData = new MeterData(responseObject.getInteger("active_power_w"),
                responseObject.getInteger("active_power_average_w"),
                (long)(responseObject.getDouble("total_power_import_kwh").doubleValue() * 1000));

        return meterData;
    }

    @Override
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
