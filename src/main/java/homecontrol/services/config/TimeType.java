package homecontrol.services.config;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public enum TimeType {
    OFF,
    PEAK;

    public static TimeType getTimeType(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        System.out.println(now);
        if (now.getDayOfWeek().equals(DayOfWeek.SATURDAY) || now.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return OFF;
        }
        if (now.getHour() >= 22 || now.getHour() < 7) {
            return OFF;
        }
        return PEAK;
    }
}
