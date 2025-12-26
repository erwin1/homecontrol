package homecontrol.metrics;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
public class MetricsServiceTest {
    static final ZonedDateTime startTS = ZonedDateTime.parse("2025-01-01T00:00:00+0100", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZ"));

    @InjectMock
    private MetricsLogger metricsLogger;

    @Inject
    private MetricsService metricsService;

    @BeforeEach
    public void setup() {
        when(metricsLogger.getCharger()).thenReturn(createList());
        when(metricsLogger.getPv()).thenReturn(new LinkedList<>());
        when(metricsLogger.getGridImport()).thenReturn(new LinkedList<>());
        when(metricsLogger.getGridExport()).thenReturn(new LinkedList<>());
    }

    @Test
    public void testEVPerHour() throws IOException {
        List<CombinedMetrics> metrics = metricsService.getCombinedMetrics(startTS, startTS.plusDays(2).minusSeconds(1), "hour");
        for(CombinedMetrics m : metrics) {
            System.out.println(m.getTimestamp()+" "+m.getEvt1()+" "+m.getEvt2()+" "+m.getEV());
        }
        CombinedMetrics totals = metricsService.calculateTotals(metrics);

        System.out.println("totals 1 = " + totals.getEvt1());
        System.out.println("totals 2 = " + totals.getEvt2());
        System.out.println("totals = " + totals.getEV());

        assertEquals(48, metrics.size());
        assertEquals(totals.getEvt1(), new BigDecimal("6"));
        assertEquals(totals.getEvt2(), new BigDecimal("3.6"));
        assertEquals(totals.getEV(), new BigDecimal("9.6"));
    }

    @Test
    public void testEVPerDay() throws IOException {
        List<CombinedMetrics> metrics = metricsService.getCombinedMetrics(startTS, startTS.plusDays(2).minusSeconds(1), "day");
        for(CombinedMetrics m : metrics) {
            System.out.println(m.getTimestamp()+" "+m.getEvt1()+" "+m.getEvt2()+" "+m.getEV());
        }
        CombinedMetrics totals = metricsService.calculateTotals(metrics);

        System.out.println("totals 1 = " + totals.getEvt1());
        System.out.println("totals 2 = " + totals.getEvt2());
        System.out.println("totals = " + totals.getEV());

        assertEquals(2, metrics.size());
        assertEquals(totals.getEvt1(), new BigDecimal("6"));
        assertEquals(totals.getEvt2(), new BigDecimal("3.6"));
        assertEquals(totals.getEV(), new BigDecimal("9.6"));
    }

    @Test
    public void testEVPerMonth() throws IOException {
        List<CombinedMetrics> metrics = metricsService.getCombinedMetrics(startTS, startTS.plusMonths(1).minusSeconds(1), "month");
        for(CombinedMetrics m : metrics) {
            System.out.println(m.getTimestamp()+" "+m.getEvt1()+" "+m.getEvt2()+" "+m.getEV());
        }
        CombinedMetrics totals = metricsService.calculateTotals(metrics);

        System.out.println("totals 1 = " + totals.getEvt1());
        System.out.println("totals 2 = " + totals.getEvt2());
        System.out.println("totals = " + totals.getEV());

        assertEquals(1, metrics.size());
        assertEquals(totals.getEvt1(), new BigDecimal("69"));
        assertEquals(totals.getEvt2(), new BigDecimal("79.8"));
        assertEquals(totals.getEV(), new BigDecimal("148.8"));
    }

    @Test
    public void testEVPerMonthFilterNoLabel() throws IOException {
        List<Metrics> list = metricsService.getMetrics("charger", startTS, startTS.plusMonths(1).minusSeconds(1), "day", null);
        assertEquals(31, list.size());
        int t1 = list.stream().mapToInt(m -> m.getValuet1().multiply(BigDecimal.valueOf(1000)).intValue()).sum();
        int t2 = list.stream().mapToInt(m -> m.getValuet2().multiply(BigDecimal.valueOf(1000)).intValue()).sum();
        assertEquals(BigDecimal.valueOf(t1).divide(BigDecimal.valueOf(1000)), new BigDecimal("69"));
        assertEquals(BigDecimal.valueOf(t2).divide(BigDecimal.valueOf(1000)), new BigDecimal("79.8"));
    }

    @Test
    public void testEVPerMonthFilterLabel1() throws IOException {
        List<Metrics> list = metricsService.getMetrics("charger", startTS, startTS.plusMonths(1).minusSeconds(1), "day", "label1");
        assertEquals(31, list.size());
        int t1 = list.stream().mapToInt(m -> m.getValuet1().multiply(BigDecimal.valueOf(1000)).intValue()).sum();
        int t2 = list.stream().mapToInt(m -> m.getValuet2().multiply(BigDecimal.valueOf(1000)).intValue()).sum();

        assertEquals(BigDecimal.valueOf(t1+t2).divide(BigDecimal.valueOf(1000)), new BigDecimal("12.4"));
    }

    @Test
    public void testEVPerMonthFilterLabel2() throws IOException {
        List<Metrics> list = metricsService.getMetrics("charger", startTS, startTS.plusMonths(1).minusSeconds(1), "day", "label2");
        assertEquals(31, list.size());
        int t1 = list.stream().mapToInt(m -> m.getValuet1().multiply(BigDecimal.valueOf(1000)).intValue()).sum();
        int t2 = list.stream().mapToInt(m -> m.getValuet2().multiply(BigDecimal.valueOf(1000)).intValue()).sum();
        assertEquals(BigDecimal.valueOf(t1+t2).divide(BigDecimal.valueOf(1000)), new BigDecimal("3.1"));
    }

    private List<Metrics> createList() {
        List<Metrics> list = new LinkedList<>();
        ZonedDateTime ts = startTS;
        ZonedDateTime endTS = startTS.plusMonths(2);
        while(ts.isBefore(endTS)) {
            BigDecimal v = new BigDecimal("0.1");
            String label = null;
//            System.out.println("HOUR = "+ts.getHour());
            if (ts.getHour() == 13 && ts.getMinute() == 0) {
                label = "label2";
            } else if (ts.getHour() == 12 || ts.getHour() == 14) {
                label = "label1";
            }
            list.add(new Metrics(ts, null, inPeakHours(ts) ? v : BigDecimal.ZERO, inPeakHours(ts) ? BigDecimal.ZERO : v, label));
            ts = ts.plusMinutes(30);
        }
        return list;
    }

    private boolean inPeakHours(ZonedDateTime time) {
        if (time.getDayOfWeek().equals(DayOfWeek.SATURDAY) || time.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return false;
        }
        if (time.getHour() >= 22 || time.getHour() < 7) {
            return false;
        }
        return true;
    }
}
