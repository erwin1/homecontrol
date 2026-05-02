package homecontrol.services.powercontrol;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * State machine for PV-only EV charging
 * Features:
 *   - Keep charging when grid export is above threshold but below minimum charging (to allow for grim import so charing can continue at minimum power)
 *   - Delay in stopping charging when dropping below minimum charging:
 *      - Keep charging at same power in the first run
 *      - Keep charging at minimum charging power for maxHoldAtMinCharging minutes (to limit the number of charging start/stops)
 */
@ApplicationScoped
public class OptimalExportChargingService {

    private static final Logger log = Logger.getLogger(OptimalExportChargingService.class.getName());


    private boolean flag1 = false;

    private boolean flag2 = false;

    private int holdAtMinChargingCounter = 0;

    public OptimalExportChargingService() {
    }

    /**
     * Process one measurement cycle and return the new target charge current.
     *
     * @return New target charge current in amps (0 = stop charging).
     */
    public int tick(int gridPowerW, int currentChargingW, int allowedImportW, float voltage, int minChargeAmps, int maxChargeAmps, int solarYieldW, int minSurplusW, int solarYieldThresholdW, int maxHoldAtMinCharging) {

        int currentChargingA = Math.round(currentChargingW / voltage);

        if (gridPowerW <= 0) {
            // -----------------------------------------------------------------
            // Surplus: exporting or exactly balanced
            // -----------------------------------------------------------------
            int surplusWatts = -gridPowerW; // export is surplus available to charger
            int totalAvailableForChargingWatts = currentChargingW + surplusWatts;

            int totalAvailableForChargingAmps = Math.round(totalAvailableForChargingWatts / voltage);

            if (totalAvailableForChargingAmps >= minChargeAmps) {
                if (currentChargingA == totalAvailableForChargingWatts) {
                    log.log(Level.INFO, "Surplus {0}W → No change needed charging at {1}A", new Object[]{surplusWatts, currentChargingA});
                } else {
                    currentChargingA = Math.min(totalAvailableForChargingAmps, maxChargeAmps);
                    log.log(Level.INFO, "Surplus {0}W → charging at {1}A", new Object[]{surplusWatts, currentChargingA});
                }
            } else if (solarYieldThresholdW > 0 && solarYieldW >= solarYieldThresholdW && totalAvailableForChargingWatts >= minSurplusW) {
                // Surplus below minChargeAmps but solar is producing well → charge at minimum
                // to consume remaining solar rather than exporting it
                currentChargingA = minChargeAmps;
                log.log(Level.INFO, "Surplus {0}W below minimum but solar {1}W >= threshold {2}W → charging at {3}A",
                        new Object[]{surplusWatts, solarYieldW, solarYieldThresholdW, minChargeAmps});
            } else {
                log.log(Level.FINE, "Surplus {0}W insufficient to start charging (need {1}W)",
                        new Object[]{surplusWatts, minChargeAmps * voltage});
            }

            clearFlags();

        } else {
            // -----------------------------------------------------------------
            // Import: grid is supplying power
            // -----------------------------------------------------------------
            int importAmps = Math.round(gridPowerW / voltage);
            int throttledAmps = currentChargingA - importAmps;
            int gridPowerWithoutChargingW = gridPowerW - currentChargingW;

            if (throttledAmps >= minChargeAmps) {
                // -----------------------------------------------------------------
                // Throttle: result stays at or above minChargeAmps — straightforward reduction
                // -----------------------------------------------------------------
                if (currentChargingA == throttledAmps) {
                    log.log(Level.INFO, "Import {0}W → No change need charging at {1}A", new Object[]{gridPowerW, currentChargingA});
                } else {
                    currentChargingA = throttledAmps;
                    log.log(Level.INFO, "Import {0}W → throttling to {1}A", new Object[]{gridPowerW, currentChargingA});
                }
                clearFlags();

            } else if (solarYieldThresholdW > 0 && solarYieldW >= solarYieldThresholdW && gridPowerWithoutChargingW < 0 && -gridPowerWithoutChargingW > minSurplusW) {
                currentChargingA = minChargeAmps;
                log.log(Level.INFO, "Surplus {0}W below minimum but solar {1}W >= threshold {2}W → charging at {3}A",
                        new Object[]{-gridPowerWithoutChargingW, solarYieldW, solarYieldThresholdW, minChargeAmps});
            } else {
                // -----------------------------------------------------------------
                // Throttle would go below minChargeAmps (or we're already at/below it).
                // -----------------------------------------------------------------

                if (currentChargingA <= 0) {
                    log.log(Level.INFO, "Import {0}W - not charging.", new Object[]{gridPowerW});
                } else if (!flag1 && !flag2) {
                    // First tick below threshold — hold, set flag 1
                    log.log(Level.INFO, "Import {0}W would push below {1}A (throttled={2}A) — holding at {3}A, flag1 set",
                            new Object[]{gridPowerW, minChargeAmps, throttledAmps, currentChargingA});
                    flag1 = true;

                } else if (flag1 && !flag2) {
                    // Second tick — drop to minChargeAmps, start hold counter, set flag 2
                    currentChargingA = minChargeAmps;
                    flag1 = false;
                    flag2 = true;
                    holdAtMinChargingCounter = 1;
                    log.log(Level.INFO, "Import persists — set to {0}A, flag2 set, holdCounter={1}/{2}",
                            new Object[]{minChargeAmps, holdAtMinChargingCounter, maxHoldAtMinCharging});

                } else if (flag2 && holdAtMinChargingCounter < maxHoldAtMinCharging) {
                    // Holding at minChargeAmps — increment counter
                    holdAtMinChargingCounter++;
                    log.log(Level.INFO, "Holding at {0}A, holdCounter={1}/{2}", new Object[]{minChargeAmps, holdAtMinChargingCounter, maxHoldAtMinCharging});

                } else {
                    // Hold period exhausted — stop charging
                    log.log(Level.INFO, "Hold period exhausted (holdCounter={0}/{1}) — stopping charging",
                            new Object[]{holdAtMinChargingCounter, maxHoldAtMinCharging});
                    currentChargingA = 0;
                    clearFlags();
                }

                if (currentChargingA != 0) {
                    if (currentChargingA * voltage > allowedImportW) {
                        if (minChargeAmps * voltage > allowedImportW) {
                            log.log(Level.INFO, "{0}A and {1}A (minimum charging) are above allowed import {2}. stopping charging.", new Object[]{currentChargingA, minChargeAmps, allowedImportW});
                            currentChargingA = 0;
                            clearFlags();
                        } else {
                            log.log(Level.INFO, "{0}A is above allowed import {1}. set charging to minimum {2}.", new Object[]{currentChargingA, allowedImportW, minChargeAmps});
                            currentChargingA = minChargeAmps;
                        }
                    }
                }

            }
        }

        return currentChargingA;
    }


    public void reset() {
        clearFlags();
        log.info("State machine reset");
    }

    private void clearFlags() {
        flag1 = false;
        flag2 = false;
        holdAtMinChargingCounter = 0;
    }
}