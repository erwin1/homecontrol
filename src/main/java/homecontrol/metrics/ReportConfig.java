package homecontrol.metrics;

import io.smallrye.config.ConfigMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "report")
public interface ReportConfig {

    List<VehicleReportConfig> vehicles();

    interface VehicleReportConfig {
        /** Must match the label stored on charging sessions in metrics data. */
        String label();
        String to();
        String vin();
        /** Tariff in c€/kWh. Defaults to 31.32 (CREG) if not set. */
        Optional<BigDecimal> tariff();
    }
}