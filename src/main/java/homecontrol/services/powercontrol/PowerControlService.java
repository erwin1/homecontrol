package homecontrol.services.powercontrol;

import homecontrol.impl.tesla.TeslaEV;
import homecontrol.impl.tesla.TeslaException;
import homecontrol.services.config.ConfigService;
import homecontrol.services.config.EVChargingStrategy;
import homecontrol.services.config.Mode;
import homecontrol.services.config.TimeType;
import homecontrol.services.ev.Charger;

import homecontrol.services.ev.EVState;
import homecontrol.services.ev.StateRefresh;
import homecontrol.services.notications.NotificationService;
import homecontrol.services.powermeter.ActivePower;
import homecontrol.services.powermeter.ElectricalPowerMeter;
import homecontrol.services.solar.Inverter;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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


    @Scheduled(every = "1m", concurrentExecution = SKIP)
    public void controlEVCharging() {
        try {
            if (configService.getCurrentMode().equals(Mode.OFF)) {
                LOGGER.log(Level.INFO, "mode is OFF");
                return;
            }
            //STAP 0
            Charger.State state = charger.getCurrentState(StateRefresh.REFRESH_ALWAYS);
            boolean changedChargerState = evControlService.handleChargerState(state);
            if (state.equals(Charger.State.NotConnected)) {
                LOGGER.log(Level.INFO, "Charger Not Connected");
                return;
            }

            EVState evState = evControlService.getCurrentState(StateRefresh.CACHED);

            //STAP 1
            if (evState != null && evState.getBattery_level() >= evState.getCharge_limit_soc()) {
                LOGGER.log(Level.INFO, "Already at charging limit.");
                return;
            }

            //STAP 2
            int currentBatteryLevel = 0;//default to 0% in case of unknown vehicle - so the charging strategy will be based on (off/)peak time only
            if (evState != null) {
                currentBatteryLevel = evState.getBattery_level();
            }
            EVChargingStrategy evChargingStrategy = configService.getEVChargingConfigs().getEVChargingStrategy(
                    currentBatteryLevel,
                    TimeType.getTimeType(clock));


            //STAP 3
            Uni<ActivePower> activePowerUni = electricalPowerMeter.getActivePower();
            Uni<Integer> solarYieldUni = inverter.getCurrentYield();
            Uni<Integer> chargingUni = charger.getActivePower();

            var tuple = Uni.combine().all().unis(
                    activePowerUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read activePower timeout")),
                    solarYieldUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read solarYield timeout")),
                    chargingUni.ifNoItem().after(Duration.ofSeconds(30)).failWith(new RuntimeException("read charger timeout"))
            ).asTuple().await().atMost(Duration.ofSeconds(31));

            int chargeAmps = powerCalculator.calculateOptimalChargingA(evChargingStrategy,
                    tuple.getItem1(),
                    powerPeakService.getCurrentMonth15minPeak(),
                    tuple.getItem2(),
                    tuple.getItem3(),
                    configService.getMinimumChargingA());

            //STAP 4
            evControlService.changeCharging(chargeAmps);

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
                notificationService.sendNotification("Error in homecontrol: " + e.getMessage() + " " + e + " " + e.getStackTrace()[0].toString());
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
