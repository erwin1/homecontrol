package homecontrol.services.powercontrol;

import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.powermeter.ActivePower;
import jakarta.enterprise.context.RequestScoped;

import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class PowerCalculator {
    public static final Logger LOGGER = Logger.getLogger(PowerCalculator.class.getName());

    public int calculateOptimalChargingA(EVChargingStrategy chargingStrategy, ActivePower activePower, int homeBatteryPowerW, int homeBatterySoCPct, int currentMonth15minPeak, int solarYieldW, int chargingW, int minimumChargingA, OptimalExportChargingService optimalExportChargingService) {
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

        int exportWhenChargingIsExcluded = Math.max(0, chargingW - activePower.getActivePower() + Math.min(0, homeBatteryPowerW));

        int currentChargeAmps = Math.round(chargingW/voltage);//TODO this could be lower than the minimum when charging is just being started!!

        if (!(chargingStrategy.getType().equals(EVChargingStrategy.Type.EXP_OPT1) || chargingStrategy.getType().equals(EVChargingStrategy.Type.EXP_OPT2))) {
            optimalExportChargingService.reset();
        }

        int chargeAtAmps = switch (chargingStrategy.getType()) {
            case EXP -> Math.round(exportWhenChargingIsExcluded / voltage);
            case EXP_OPT1 -> optimalExportChargingService.tick(activePower.getActivePower(), homeBatteryPowerW, homeBatterySoCPct, chargingW, powerDifferenceW, voltage, minimumChargingA, 32, solarYieldW, chargingStrategy.getMinPowerExportW(), chargingStrategy.getMinSolarYieldW(), chargingStrategy.getMaxHoldAtMinCharging());
            case EXP_OPT2 -> optimalExportChargingService.tick(activePower.getActivePower(), homeBatteryPowerW, homeBatterySoCPct, chargingW, powerDifferenceW, voltage, minimumChargingA, 32, solarYieldW, chargingStrategy.getMinPowerExportW(), chargingStrategy.getMinSolarYieldW(), chargingStrategy.getMaxHoldAtMinCharging());
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
                if (solarYieldW == 0 && chargingW > 0) {
                    int diffAmps = newChargingAmps - currentChargeAmps;
                    if (diffAmps > 0) {
                        newChargingAmps = currentChargeAmps;
                    }
                }
                yield newChargingAmps;
            }
        };
        chargeAtAmps = Math.max(chargeAtAmps, 0);
        if (chargeAtAmps < minimumChargingA) {
            chargeAtAmps = 0;
        }
        chargeAtAmps = Math.min(chargeAtAmps, 32);

        LOGGER.log(Level.INFO, "\n-=-=-= Current values =-=-=-=-\n" +
                        "\tActive power from grid: {0}W\n" +
                        "\tActive power to grid: {1}W\n" +
                        "\tActive power average: {2}W\n" +
                        "\tActive solar yield: {3}W\n" +
                        "\tActive EV charging: {4}W\n" +
                        "\tActive home battery power: {16}W\n" +
                        "\tHome battery SoC: {17}%\n" +
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
                        chargeAtAmps*voltage,
                        homeBatteryPowerW,
                        homeBatterySoCPct
                });


        return chargeAtAmps;
    }


}
