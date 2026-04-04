package homecontrol.impl.hwep1;

import homecontrol.services.powermeter.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class HWEP1PowerMeter implements ElectricalPowerMeter {
    @Inject
    private HWEP1Client hwep1Client;

    @Override
    @Retry(maxRetries = 10, delay = 3, delayUnit = ChronoUnit.SECONDS)
    public MeterReading getCurrentReading() {
        Telegram telegram = null;
        try {
            telegram = hwep1Client.getTelegram();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MeterReading data = getMeterReading(telegram);

        return data;
    }


    @Override
    @Asynchronous
    @Retry(maxRetries = 3, delay = 5000)
    public Uni<ActivePower> getActivePower() {
        Telegram telegram = null;
        try {
            telegram = hwep1Client.getTelegram();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ActivePower activePower = getActivePower(telegram);

        return Uni.createFrom().item(activePower);
    }

    @Override
    @Asynchronous
    @Retry(maxRetries = 3, delay = 5000)
    public Uni<Power> getPower() {
        Telegram telegram = null;
        try {
            telegram = hwep1Client.getTelegram();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Uni.createFrom().item(new Power(getActivePower(telegram), getMeterReading(telegram), getMonthlyPowerPeak(telegram)));
    }

    @Override
    @Retry(maxRetries = 3, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public MonthlyPowerPeak getMonthlyPowerPeak() {
        Telegram telegram = null;
        try {
            telegram = hwep1Client.getTelegram();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return getMonthlyPowerPeak(telegram);
    }


    private ActivePower getActivePower(Telegram telegram) {
        ActivePower activePower = new ActivePower(LocalDateTime.parse(telegram.getTimestamp(),
                DateTimeFormatter.ofPattern("yyMMddHHmmss")).atZone(ZoneId.of("Europe/Brussels")),
                telegram.getActive_power_import_w() - telegram.getActive_power_export_w(),
                telegram.getActive_power_average_w(),
                telegram.getActive_voltage_v());
        return activePower;
    }

    private MeterReading getMeterReading(Telegram telegram) {
        MeterReading data = new MeterReading();

        data.setTotal_power_import_kwh(telegram.getTotal_power_import_kwh());
        data.setTotal_power_import_t1_kwh(telegram.getTotal_power_import_t1_kwh());
        data.setTotal_power_import_t2_kwh(telegram.getTotal_power_import_t2_kwh());
        data.setTotal_power_export_kwh(telegram.getTotal_power_export_kwh());
        data.setTotal_power_export_t1_kwh(telegram.getTotal_power_export_t1_kwh());
        data.setTotal_power_export_t2_kwh(telegram.getTotal_power_export_t2_kwh());
        data.setTotal_gas_m3(telegram.getTotal_gas_m3());
        return data;
    }



    private MonthlyPowerPeak getMonthlyPowerPeak(Telegram telegram) {
        return new MonthlyPowerPeak(LocalDateTime.parse(telegram.getMontly_power_peak_timestamp(),
                DateTimeFormatter.ofPattern("yyMMddHHmmss")).atZone(ZoneId.of("Europe/Brussels")),
                telegram.getMontly_power_peak_w());
    }
}
