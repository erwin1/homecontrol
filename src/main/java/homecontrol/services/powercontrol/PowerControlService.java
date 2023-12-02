package homecontrol.services.powercontrol;

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

            EVState evState = evControlService.getCurrentState(changedChargerState ? StateRefresh.REFRESH_ALWAYS : StateRefresh.CACHED);

            if (evState.getCharging_state().equals("Charging") && !state.equals(Charger.State.InProgress)) {
                LOGGER.severe("inconsistent charging state. refreshing ev state.");
                evState = evControlService.getCurrentState(StateRefresh.REFRESH_ALWAYS);
                if (evState.getCharging_state().equals("Charging") && !state.equals(Charger.State.InProgress)) {
                    notificationService.sendNotification("Inconsistent charging/charger state: EV="+evState.getCharging_state()+" Charger="+state);
                }
            }

            //STAP 1
            if (evState.getBattery_level() >= evState.getCharge_limit_soc()) {
                LOGGER.log(Level.INFO, "Already at charging limit.");
                return;
            }

            //STAP 2
            EVChargingStrategy evChargingStrategy = configService.getEVChargingConfigs().getEVChargingStrategy(
                    evState.getBattery_level(),
                    TimeType.getTimeType(clock));


            //STAP 3
            Uni<ActivePower> activePowerUni = electricalPowerMeter.getActivePower();
            Uni<Integer> solarYieldUni = inverter.getCurrentYield();
            Uni<Integer> chargingUni = charger.getActivePower();

            var tuple = Uni.combine().all().unis(activePowerUni, solarYieldUni, chargingUni).asTuple().await().atMost(Duration.ofSeconds(30));

            int chargeAmps = powerCalculator.calculateOptimalChargingA(evChargingStrategy,
                    tuple.getItem1(),
                    powerPeakService.getCurrentMonth15minPeak(),
                    tuple.getItem2(),
                    tuple.getItem3(),
                    evState.getCharge_amps(),
                    configService.getMinimumChargingA());

            //STAP 4
            evControlService.changeCharging(chargeAmps);

        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error in homecontrol", e);
            notificationService.sendNotification("Error in homecontrol "+e+" "+e.getStackTrace()[0].toString());
        }
    }


}
