package homecontrol.metrics;

import io.smallrye.config.ConfigMapping;

import java.util.List;

@ConfigMapping(prefix = "report")
public interface ReportConfig {

    List<VehicleReportConfig> vehicles();

    interface VehicleReportConfig {
        /** Must match the label stored on charging sessions in metrics data. */
        String label();
        String to();
        String vin();
    }
}