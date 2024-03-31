package homecontrol.services.ev;

import java.time.Duration;

public enum StateRefresh {
    CACHED_OR_NULL,
    CACHED(Duration.ofMinutes(60), Duration.ofMinutes(15)),
    REFRESH_IF_ONLINE,
    REFRESH_ALWAYS;

    StateRefresh() {
    }

    StateRefresh(Duration maxCacheTime, Duration maxCacheTimeIfOnline) {
        this.maxCacheTime = maxCacheTime;
        this.maxCacheTimeIfOnline = maxCacheTimeIfOnline;
    }

    private Duration maxCacheTime;
    private Duration maxCacheTimeIfOnline;

    public Duration getMaxCacheTime() {
        return maxCacheTime;
    }

    public Duration getMaxCacheTimeIfOnline() {
        return maxCacheTimeIfOnline;
    }
}
