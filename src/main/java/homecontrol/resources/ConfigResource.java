package homecontrol.resources;

import homecontrol.impl.sma.SMACharger;
import homecontrol.metrics.ReportConfig;
import homecontrol.services.config.ConfigService;
import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.config.Mode;
import homecontrol.services.config.PeakStrategy;
import homecontrol.services.notications.NotificationType;
import homecontrol.services.powercontrol.PowerPeakService;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

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
    ReportConfig reportConfig;

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
                .data("minimumPVYieldExp1", configService.getMinimumPVYieldExp1())
                .data("minimumExportExp1", configService.getMinimumExportExp1())
                .data("maxHoldAtMinChargingExp1", configService.getMaxHoldAtMinChargingExp1())
                .data("minimumPVYieldExp2", configService.getMinimumPVYieldExp2())
                .data("minimumExportExp2", configService.getMinimumExportExp2())
                .data("maxHoldAtMinChargingExp2", configService.getMaxHoldAtMinChargingExp2())
                .data("tariffEntries", configService.getTariffEntries())
                .data("allNotificationTypes", NotificationType.values())
                .data("enabledNotificationTypes", configService.getEnabledNotificationTypes());
    }

    @POST
    @Path("save")
    public Response changeMode(@FormParam("mode") Mode mode,
                               @FormParam("strategy") PeakStrategy peakStrategy,
                               @FormParam("batteryLevelX") int batteryLevelX,
                               @FormParam("batteryLevelY") int batteryLevelY,
                               @FormParam("minimumPVYieldExp1") int minimumPVYieldExp1,
                               @FormParam("minimumExportExp1") int minimumExportExp1,
                               @FormParam("maxHoldAtMinChargingExp1") int maxHoldAtMinChargingExp1,
                               @FormParam("minimumPVYieldExp2") int minimumPVYieldExp2,
                               @FormParam("minimumExportExp2") int minimumExportExp2,
                               @FormParam("maxHoldAtMinChargingExp2") int maxHoldAtMinChargingExp2,
                               @FormParam("p1") EVChargingStrategy.Type p1,
                               @FormParam("p2") EVChargingStrategy.Type p2,
                               @FormParam("p3") EVChargingStrategy.Type p3,
                               @FormParam("o1") EVChargingStrategy.Type o1,
                               @FormParam("o2") EVChargingStrategy.Type o2,
                               @FormParam("o3") EVChargingStrategy.Type o3,
                               @FormParam("notificationTypes") List<NotificationType> notificationTypes
                               ) {
        if (mode.equals(Mode.OFF) && configService.getCurrentMode().equals(Mode.ON)) {
            //when turned off, make sure to set default settings to the charger
            charger.startCharging();
            charger.changeChargingAmps(32);
        }
        configService.setCurrentMode(mode);
        configService.setBatteryLevelX(batteryLevelX);
        configService.setBatteryLevelY(batteryLevelY);
        configService.setPeakStrategy(peakStrategy);
        configService.setMinimumPVYieldExp1(minimumPVYieldExp1);
        configService.setMinimumPVYieldExp2(minimumPVYieldExp2);
        configService.setMinimumExportExp1(minimumExportExp1);
        configService.setMinimumExportExp2(minimumExportExp2);
        configService.setMaxHoldAtMinChargingExp1(maxHoldAtMinChargingExp1);
        configService.setMaxHoldAtMinChargingExp2(maxHoldAtMinChargingExp2);
        configService.setP1(p1);
        configService.setP2(p2);
        configService.setP3(p3);
        configService.setO1(o1);
        configService.setO2(o2);
        configService.setO3(o3);

        configService.setEnabledNotificationTypes(
                notificationTypes == null || notificationTypes.isEmpty()
                        ? EnumSet.noneOf(NotificationType.class)
                        : EnumSet.copyOf(notificationTypes));
        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

    @POST
    @Path("saveTariff")
    public Response saveTariff(@FormParam("effectiveFrom") String effectiveFrom,
                               @FormParam("tariff") String tariff) {
        configService.setTariff(LocalDate.parse(effectiveFrom), new BigDecimal(tariff));
        configService.persist();
        return Response.seeOther(URI.create("/config")).build();
    }

}