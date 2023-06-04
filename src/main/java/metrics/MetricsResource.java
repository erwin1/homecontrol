package metrics;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/metrics")
public class MetricsResource {
    @Inject
    MetricsService metricsService;

    @GET
    @Path("live")
    @Produces(MediaType.APPLICATION_JSON)
    public LiveMetrics live() {
        return metricsService.getLiveMetrics();
    }

    @GET
    @Path("daily")
    @Produces(MediaType.APPLICATION_JSON)
    public Totals dailyStatus(@QueryParam("date") String date) throws IOException {
        ZonedDateTime z1 = ZonedDateTime.of(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIDNIGHT, ZoneId.of("Europe/Brussels"));
        List<CombinedMetrics> details = metricsService.getCombinedMetrics(z1, z1.plusDays(1).minusSeconds(1), "hour");
        CombinedMetrics total = metricsService.calculateTotals(details);
        Totals totals = new Totals();
        totals.setDetails(details);
        totals.setTotals(total);
        return totals;
    }

    @GET
    @Path("monthly")
    @Produces(MediaType.APPLICATION_JSON)
    public Totals monthlyStatus(@QueryParam("date") String date) throws IOException {
        ZonedDateTime z1 = ZonedDateTime.of(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIDNIGHT, ZoneId.of("Europe/Brussels")).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        List<CombinedMetrics> details = metricsService.getCombinedMetrics(z1, z1.plusMonths(1).minusSeconds(1), "day");
        CombinedMetrics total = metricsService.calculateTotals(details);
        Totals totals = new Totals();
        totals.setDetails(details);
        totals.setTotals(total);
        return totals;
    }

    @GET
    @Path("range")
    @Produces(MediaType.APPLICATION_JSON)
    public Totals rangeStatus(@QueryParam("start") String start, @QueryParam("end") String end) throws IOException {
        ZonedDateTime z1 = ZonedDateTime.of(LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIDNIGHT, ZoneId.of("Europe/Brussels")).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime z2 = ZonedDateTime.of(LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIDNIGHT, ZoneId.of("Europe/Brussels")).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).plusMonths(1).minusSeconds(1);
        List<CombinedMetrics> details = metricsService.getCombinedMetrics(z1, z2, "month");
        CombinedMetrics total = metricsService.calculateTotals(details);
        Totals totals = new Totals();
        totals.setDetails(details);
        totals.setTotals(total);
        return totals;
    }

}
