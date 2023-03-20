package evcharging;

public enum PeakStrategy {
    /**
     * Dynamic peak based on current month usage,
     * but limited to the configured mac peak.
     */
    DYNAMIC_LIMITED,
    /**
     * Dynamic peak based on current month usage,
     * not limited by the configured mac peak.
     */
    DYNAMIC_UNLIMITED,
    /**
     * Static configured max peak.
     */
    STATIC_LIMITED;
}
