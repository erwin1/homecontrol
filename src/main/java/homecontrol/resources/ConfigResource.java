package homecontrol.resources;

import homecontrol.impl.sma.SMACharger;
import homecontrol.services.config.ConfigService;
import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.config.Mode;
import homecontrol.services.config.PeakStrategy;
import homecontrol.services.powercontrol.PowerPeakService;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/config")
public class ConfigResource {
    @Inject
    SMACharger charger;
    @Inject
    ElectricalPowerMeter powerMeter;

    @Inject
    ConfigService configService;

    @Inject
    PowerPeakService powerPeakService;

    @Inject
    Template config;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance config() {
        return config
                .data("mode", configService.getCurrentMode())
                .data("max15minpeak", configService.getMax15minPeak())
                .data("actualmax15minpeak", powerPeakService.getCurrentMonth15minPeak())
                .data("min15minpeak", configService.getMin15minPeak())
                .data("peakstrategy", configService.getPeakStrategy())
                .data("currentmonth15minpeak", powerMeter.getMonthlyPowerPeak())
                .data("batteryLevelX", configService.getBatteryLevelX())
                .data("batteryLevelY", configService.getBatteryLevelY())
                .data("p1", configService.getP1())
                .data("p2", configService.getP2())
                .data("p3", configService.getP3())
                .data("o1", configService.getO1())
                .data("o2", configService.getO2())
                .data("o3", configService.getO3())
                .data("minimumPVYield", configService.getMinimumPVYield())
                .data("minimumExport", configService.getMinimumExport());
    }

    @POST
    @Path("save")
    public Response changeMode(@FormParam("mode") Mode mode,
                               @FormParam("strategy") PeakStrategy peakStrategy,
                               @FormParam("batteryLevelX") int batteryLevelX,
                               @FormParam("batteryLevelY") int batteryLevelY,
                               @FormParam("minimumPVYield") int minimumPVYield,
                               @FormParam("minimumExport") int minimumExport,
                               @FormParam("p1") EVChargingStrategy.Type p1,
                               @FormParam("p2") EVChargingStrategy.Type p2,
                               @FormParam("p3") EVChargingStrategy.Type p3,
                               @FormParam("o1") EVChargingStrategy.Type o1,
                               @FormParam("o2") EVChargingStrategy.Type o2,
                               @FormParam("o3") EVChargingStrategy.Type o3
                               ) {
        configService.setCurrentMode(mode);
        configService.setBatteryLevelX(batteryLevelX);
        configService.setBatteryLevelY(batteryLevelY);
        configService.setPeakStrategy(peakStrategy);
        configService.setMinimumPVYield(minimumPVYield);
        configService.setMinimumExport(minimumExport);
        configService.setP1(p1);
        configService.setP2(p2);
        configService.setP3(p3);
        configService.setO1(o1);
        configService.setO2(o2);
        configService.setO3(o3);

        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

}