package evcharging;

import evcharging.services.MeterReading;
import evcharging.services.PowerValues;
import evcharging.services.ElectricityMeter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.logging.Logger;


@ApplicationScoped
public class PowerEstimationService {
    public static final Logger LOGGER = Logger.getLogger(PowerEstimationService.class.getName());

    @Inject
    ElectricityMeter meter;

    @Inject
    ConfigService configService;

    @Inject
    PeakService peakService;

    public int calculateCurrentPowerDifference(Mode mode) {
        return switch (mode) {
            case OPTIMAL -> calculatePowerDifferenceForOptimalPeakUsage();
            case PV_ONLY -> calculatePVPowerDifference();
            case OFF -> 0;
        };
    }

    private int calculatePowerDifferenceForOptimalPeakUsage() {
        ZonedDateTime startOfPeriod = ZonedDateTime.now();
        startOfPeriod = startOfPeriod.minusMinutes(startOfPeriod.getMinute() % 15).withSecond(0).withNano(0);
        long startOfPeriodEpoch = startOfPeriod.toEpochSecond();
        long meterReadingAtPeriodStart = meter.getConsumptionMeterReadingAt(startOfPeriod);
        long currentMeterReading = meter.getCurrentConsumptionMeterReading();
        long nowEpoch = ZonedDateTime.now().toEpochSecond();
        int usageInPeriodWh = (int) (currentMeterReading - meterReadingAtPeriodStart);

        int passedTimeInSec = (int) (nowEpoch - startOfPeriodEpoch);
        int remainingTimeInSec = (15 * 60) - passedTimeInSec;

        int maxUsageInPeriodnWh = getCurrentMonth15minPeak() / 4;

        PowerValues powerValues = meter.getCurrentValues();

        int currentPowerFromGridW = powerValues.getFromGrid();
        int currentPowerToGridW = powerValues.getToGrid();

        int estimateRemainingUsageInPeriodWh = (int) (currentPowerFromGridW / 3600. * remainingTimeInSec);
        int estimateRemainingInjectionInPeriodWh = (int) (currentPowerToGridW / 3600. * remainingTimeInSec);

        int remainingUsageInPeriodWh = maxUsageInPeriodnWh - usageInPeriodWh - estimateRemainingUsageInPeriodWh;

        int powerDifferenceW = (int) (((remainingUsageInPeriodWh * 900. + estimateRemainingInjectionInPeriodWh * 900.) / remainingTimeInSec) * 4);
        int powerDifferenceA = powerDifferenceW / 220;

        LOGGER.info(startOfPeriod+" "+passedTimeInSec+"s @ "+usageInPeriodWh+"Wh || "+remainingTimeInSec+"s @ "+remainingUsageInPeriodWh+"Wh || current available: "+powerDifferenceW+"W || current from grid: "+currentPowerFromGridW+"W"+" || current to grid: "+currentPowerToGridW+" || difference "+powerDifferenceW+"W ("+powerDifferenceA+"A)");

        return powerDifferenceW;
    }

    private int calculatePVPowerDifference() {
        PowerValues powerValues = meter.getCurrentValues();
        int differenceW = 0;
        if (powerValues.getFromGrid() > 0) {
            differenceW = -powerValues.getFromGrid();
        }
        if (powerValues.getToGrid() > 0) {
            differenceW += powerValues.getToGrid();
        }
        LOGGER.info("difference PV power "+differenceW+"W");
        return differenceW;
    }

    public int getCurrentMonth15minPeak() {
        if (configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_LIMITED)
            || configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_UNLIMITED)) {
            MeterReading currentMonth15minUsagePeak = peakService.getCurrentMonth15minUsagePeak();
            if (currentMonth15minUsagePeak != null) {
                ZonedDateTime now = ZonedDateTime.now();
                if (now.getMonth() == currentMonth15minUsagePeak.getTimestamp().getMonth()) {
                    int currentPeak = Math.max(currentMonth15minUsagePeak.getValueInW(), configService.getMin15minPeak());
                    if (configService.getPeakStrategy().equals(PeakStrategy.DYNAMIC_LIMITED)) {
                        return Math.min(currentPeak, configService.getMax15minPeak());
                    } else {
                        return currentPeak;
                    }
                }
            }
            return configService.getMin15minPeak();
        } else {
            return configService.getMax15minPeak();
        }
    }

}
