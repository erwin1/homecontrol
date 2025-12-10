package homecontrol.services.powercontrol;

import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.powermeter.ActivePower;
import jakarta.enterprise.context.RequestScoped;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class PowerCalculator {
    public static final Logger LOGGER = Logger.getLogger(PowerCalculator.class.getName());

    public int calculateOptimalChargingA(EVChargingStrategy chargingStrategy, ActivePower activePower, int currentMonth15minPeak, int solarYieldW, int chargingW, int minimumChargingA) {
        ZonedDateTime startOfPeriod = activePower.getTimestamp().minusMinutes(activePower.getTimestamp().getMinute() % 15).withSecond(0).withNano(0);
        long startOfPeriodEpoch = startOfPeriod.toEpochSecond();

        long nowEpoch = activePower.getTimestamp().toEpochSecond();
        double usageInPeriodWh = activePower.getActivePowerAverage() / 4.;

        int passedTimeInSec = (int) (nowEpoch - startOfPeriodEpoch);
        int remainingTimeInSec = (15 * 60) - passedTimeInSec;

        double maxUsageInPeriodnWh = currentMonth15minPeak / 4.;

        int currentPowerFromGridW = activePower.getActivePower() > 0 ? activePower.getActivePower() : 0;
        int currentPowerToGridW = activePower.getActivePower() < 0 ? -activePower.getActivePower() : 0;

        double estimateRemainingUsageInPeriodWh = (currentPowerFromGridW / 3600. * remainingTimeInSec);
        double estimateRemainingInjectionInPeriodWh = (currentPowerToGridW / 3600. * remainingTimeInSec);
        double remainingUsageInPeriodWh = maxUsageInPeriodnWh - usageInPeriodWh - estimateRemainingUsageInPeriodWh;

        int powerDifferenceW = (int) (((remainingUsageInPeriodWh * 900. + estimateRemainingInjectionInPeriodWh * 900.) / remainingTimeInSec) * 4);

        float voltage = activePower.getActiveVoltage().floatValue();
        int minimumChargingW = (int) (minimumChargingA * voltage);

        int exportWhenChargingIsExcluded = Math.max(0, chargingW - activePower.getActivePower());

        int currentChargeAmps = Math.round(chargingW/voltage);

        int chargeAtAmps = switch (chargingStrategy.getType()) {
            case EXP -> Math.round(exportWhenChargingIsExcluded / voltage);
            case MAX -> (int)((chargingW + powerDifferenceW)/voltage);
            case MAX_OPT -> {
                int newChargingAmps = (int)((chargingW + powerDifferenceW)/voltage);

                if (passedTimeInSec < 60) {
                    if (chargingW > 0) {
                        yield currentChargeAmps;
                    }
                }
                if (passedTimeInSec < 120) {
                    if (newChargingAmps > minimumChargingA) {
                        yield newChargingAmps - 1;
                    }
                }
                if (chargingW > 0) {
                    int diffAmps = newChargingAmps - currentChargeAmps;
                    if (diffAmps > 0) {
                        newChargingAmps = currentChargeAmps;
                    }
                }
                yield newChargingAmps;
            }
            case EXP_PLUS -> {
                if (powerDifferenceW >= minimumChargingW && solarYieldW >= chargingStrategy.getMinSolarYieldW() &&
                        exportWhenChargingIsExcluded >= chargingStrategy.getMinPowerExportW()) {
                    if (exportWhenChargingIsExcluded < minimumChargingW) {
                        yield minimumChargingA;
                    }
                }
                yield Math.round(exportWhenChargingIsExcluded * 1f / voltage);
            }
        };
        chargeAtAmps = Math.max(chargeAtAmps, 0);
        if (chargeAtAmps < minimumChargingA) {
            chargeAtAmps = 0;
        }

        LOGGER.log(Level.INFO, "\n-=-=-= Current values =-=-=-=-\n" +
                        "\tActive power from grid: {0}W\n" +
                        "\tActive power to grid: {1}W\n" +
                        "\tActive power average: {2}W\n" +
                        "\tActive solar yield: {3}W\n" +
                        "\tActive charging: {4}W\n" +
                        "\tExport without charging: {5}W\n" +
                        "\tCurrent monthly peak: {6}W\n"+
                        "\tElapsed time in period: {7}s\n"+
                        "\tRemaining time in period: {8}s\n"+
                        " -=-=-=-=-=-=-=-=-=-=-=-=-=- \n" +
                        "\tEstimated remaining usage in period: {9}Wh\n"+
                        "\tEstimated remaining injection in period: {10}Wh\n"+
                        "\tTotal remaining allowed usage in period: {11}Wh\n"+
                        "\tPower difference: {12}W\n" +
                        "\tCharging strategy: {13}\n" +
                        "\tNew charging amps: {14}A ({15}W)\n" +
                        " -=-=-=-=-=-=-=-=-=-=-=-=-=- \n",
                new Object[]{currentPowerFromGridW,
                        currentPowerToGridW,
                        activePower.getActivePowerAverage(),
                        solarYieldW,
                        chargingW,
                        chargingW - activePower.getActivePower(),
                        currentMonth15minPeak,
                        passedTimeInSec,
                        remainingTimeInSec,
                        estimateRemainingUsageInPeriodWh,
                        estimateRemainingInjectionInPeriodWh,
                        remainingUsageInPeriodWh,
                        powerDifferenceW,
                        chargingStrategy,
                        chargeAtAmps,
                        chargeAtAmps*voltage});


        return chargeAtAmps;
    }


}
