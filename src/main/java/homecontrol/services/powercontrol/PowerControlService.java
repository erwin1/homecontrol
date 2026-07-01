package homecontrol.services.powercontrol;

import homecontrol.impl.tesla.TeslaException;
import homecontrol.metrics.DetailedMetricsService;
import homecontrol.services.config.ConfigService;
import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.config.Mode;
import homecontrol.services.config.TimeType;
import homecontrol.services.ev.Charger;

import homecontrol.services.ev.EVState;
import homecontrol.services.ev.StateRefresh;
import homecontrol.services.notications.NotificationType;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powermeter.*;
import homecontrol.services.solar.Inverter;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@ApplicationScoped
public class PowerControlService {
    public static final Logger LOGGER = Logger.getLogger(PowerControlService.class.getName());

    @Inject
    private ElectricalPowerMeter electricalPowerMeter;
    @Inject
    private HomeBatteryPowerMeter homeBatteryPowerMeter;
    @Inject
    private Inverter inverter;
    @Inject
    private Charger charger;
    @Inject
    private ConfigService configService;
    @Inject
    private NotificationService notificationService;
    @Inject
    private Clock clock;
    @Inject
    private PowerCalculator powerCalculator;
    @Inject
    private PowerPeakService powerPeakService;
    @Inject
    private EVControlService evControlService;
    @Inject
    DetailedMetricsService detailedMetricsService;
    @Inject
    OptimalExportChargingService optimalExportChargingService;
    @Inject
    BatteryControlService batteryControlService;

    boolean reportChargeLimit = false;

    @Scheduled(every = "1m", concurrentExecution = SKIP)
    public void controlEVCharging() {
        try {
            if (configService.getCurrentMode().equals(Mode.OFF)) {
                LOGGER.log(Level.INFO, "mode is OFF");
                batteryControlService.reset();
                return;
            }
            //STAP 0
            Charger.State state = charger.getCurrentState(StateRefresh.REFRESH_ALWAYS);
            boolean changedChargerState = evControlService.handleChargerState(state);
            if (state.equals(Charger.State.NotConnected)) {
                reportChargeLimit = false;
                LOGGER.log(Level.INFO, "Charger Not Connected");
                batteryControlService.reset();
                return;
            }

            EVState evState = evControlService.getCurrentState(StateRefresh.CACHED);

            //STAP 1
            if (evState != null && evState.getBattery_level() >= evState.getCharge_limit_soc()) {
                LOGGER.log(Level.INFO, "Already at charging limit.");
                if (!reportChargeLimit) {
                    reportChargeLimit = true;
                    notificationService.sendNotification(NotificationType.IMPORTANT, evControlService.getConnectedVehicle().getName()+ " fully charged");
                }
                batteryControlService.reset();
                return;
            }

            //STAP 2
            int currentBatteryLevel = 0;//default to 0% in case of unknown vehicle - so the charging strategy will be based on (off/)peak time only
            if (evState != null) {
                currentBatteryLevel = evState.getBattery_level();
            }
            LOGGER.log(Level.INFO, "Current EV battery level: "+currentBatteryLevel+"%");
            EVChargingStrategy evChargingStrategy = configService.getEVChargingConfigs().getEVChargingStrategy(
                    currentBatteryLevel,
                    TimeType.getTimeType(clock));


            //STAP 3
            Uni<ActivePower> activePowerUni = electricalPowerMeter.getActivePower();
            Uni<BatteryPower> batteryPowerUni = homeBatteryPowerMeter.getActivePower();
            Uni<BatteryMeterReading> batteryMeterUni = homeBatteryPowerMeter.getCurrentReading();
            Uni<Integer> solarYieldUni = inverter.getCurrentYield();
            Uni<Integer> chargingUni = charger.getActivePowerW();

            var tuple = Uni.combine().all().unis(
                    activePowerUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read activePower timeout")),
                    solarYieldUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read solarYield timeout")),
                    chargingUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read charger timeout")),
                    batteryPowerUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read batteries timeout")),
                    batteryMeterUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read battery measurement timeout"))
            ).asTuple().await().atMost(Duration.ofSeconds(31));


            LOGGER.info("EV STRATEGY "+evChargingStrategy +" "+tuple.getItem4().getPermissions());
            batteryControlService.set(tuple.getItem4());
            if (evChargingStrategy.getType().equals(EVChargingStrategy.Type.MAX) || evChargingStrategy.getType().equals(EVChargingStrategy.Type.MAX_OPT)) {
                batteryControlService.disableDischarging();
            } else {
                batteryControlService.reset();
            }

            int chargeAmps = powerCalculator.calculateOptimalChargingA(evChargingStrategy,
                    tuple.getItem1(),
                    tuple.getItem4().getActivePower(),
                    tuple.getItem5().getState_of_charge_pct(),
                    powerPeakService.getCurrentMonth15minPeak(),
                    tuple.getItem2(),
                    tuple.getItem3(),
                    configService.getMinimumChargingA(),
                    optimalExportChargingService);

            //STAP 4
            evControlService.changeCharging(chargeAmps, tuple.getItem1().getActiveVoltage().setScale(0, RoundingMode.HALF_UP).intValue());

        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error in homecontrol: "+e.getMessage(), e);
            boolean alert = true;
            TeslaException te = findCause(e);
            LOGGER.info("exception = "+e+" tesla exception = "+te);
            if (te != null) {
                LOGGER.info("tesla exception = "+te+" code = "+te.getCode());
                if (te.getCode() == 409) {
                    alert = false;
                }
            }
            if (alert) {
                notificationService.sendNotification(NotificationType.ERROR, "Error in homecontrol: " + e.getMessage() + " " + e + " " + e.getStackTrace()[0].toString());
            }
        }
    }

    private TeslaException findCause(Throwable e) {
        if (e instanceof TeslaException) {
            return (TeslaException) e;
        }
        if (e.getCause() != null) {
            return findCause(e.getCause());
        }
        return null;
    }


}
