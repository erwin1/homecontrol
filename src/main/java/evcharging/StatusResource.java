package evcharging;

import evcharging.impl.sma.SMACharger;
import evcharging.services.ElectricityMeter;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;


import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/status")
public class StatusResource {
    @Inject
    SMACharger charger;
    @Inject
    ElectricityMeter meter;

    @Inject
    ConfigService configService;

    @Inject
    PeakService peakService;

    @Inject
    PowerEstimationService powerEstimationService;

    @Inject
    Template status;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance status() {
        return status
                .data("chargerState", charger.getState().toString())
                .data("powerValues", meter.getCurrentValues())
                .data("mode", configService.getCurrentMode())
                .data("max15minpeak", configService.getMax15minPeak())
                .data("actualmax15minpeak", powerEstimationService.getCurrentMonth15minPeak())
                .data("min15minpeak", configService.getMin15minPeak())
                .data("peakstrategy", configService.getPeakStrategy())
                .data("currentmonth15minpeak", peakService.getCurrentMonth15minUsagePeak())
                .data("chargelimitgrid", configService.getChargeLimitFromGrid());
    }

    @POST
    @Path("changemode")
    public Response changeMode(@FormParam("mode") Mode mode) {
        System.out.println("Have to change mode to "+mode);
        configService.setCurrentMode(mode);
        return Response.seeOther(URI.create("/status")).build();
    }

    @POST
    @Path("changepeakstrategy")
    public Response changePeakStrategy(@FormParam("strategy") PeakStrategy peakStrategy) {
        System.out.println("Have to change strategy to "+peakStrategy);
        configService.setPeakStrategy(peakStrategy);
        return Response.seeOther(URI.create("/status")).build();
    }

    @POST
    @Path("changechargelimitfromgrid")
    public Response changeChargeLimitFromGrid(@FormParam("limit") int limit) {
        System.out.println("Have to charge limit to "+limit);
        configService.setChargeLimitFromGrid(limit);
        return Response.seeOther(URI.create("/status")).build();
    }

}