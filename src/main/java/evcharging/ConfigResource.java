package evcharging;

import evcharging.impl.sma.SMACharger;
import evcharging.services.ElectricityMeter;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/config")
public class ConfigResource {
    @Inject
    SMACharger charger;
    @Inject
    Instance<ElectricityMeter> meter;

    @Inject
    ConfigService configService;

    @Inject
    PowerEstimationService powerEstimationService;

    @Inject
    Template config;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance config() {
        return config
                .data("mode", configService.getCurrentMode())
                .data("max15minpeak", configService.getMax15minPeak())
                .data("actualmax15minpeak", powerEstimationService.getCurrentMonth15minPeak())
                .data("min15minpeak", configService.getMin15minPeak())
                .data("peakstrategy", configService.getPeakStrategy())
                .data("currentmonth15minpeak", meter.get().getCurrentMonthPeak())
                .data("chargelimitgrid", configService.getChargeLimitFromGrid());
    }

    @POST
    @Path("changemode")
    public Response changeMode(@FormParam("mode") Mode mode) {
        System.out.println("Have to change mode to "+mode);
        configService.setCurrentMode(mode);
        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

    @POST
    @Path("changepeakstrategy")
    public Response changePeakStrategy(@FormParam("strategy") PeakStrategy peakStrategy) {
        System.out.println("Have to change strategy to "+peakStrategy);
        configService.setPeakStrategy(peakStrategy);
        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

    @POST
    @Path("changechargelimitfromgrid")
    public Response changeChargeLimitFromGrid(@FormParam("limit") int limit) {
        System.out.println("Have to charge limit to "+limit);
        configService.setChargeLimitFromGrid(limit);
        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

}