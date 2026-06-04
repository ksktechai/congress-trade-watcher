package nz.co.ksktech.congresstrades.domain.enums;

/**
 * Categories of pattern detected by {@code SignalDetectionService}.
 *
 * <p>These are <strong>research signals only</strong>: they flag patterns worth a
 * human looking at, not buy/sell recommendations.</p>
 */
public enum SignalType {
    /** 3+ distinct members buying the same ticker inside a 14-day window. */
    CLUSTER,
    /** A trade whose size is far above the member's historical median. */
    OUTLIER,
    /** Disclosure filed close to the 45-day legal limit. */
    LATE_DISCLOSURE,
    /** A member repeatedly trading the same ticker in a short window. */
    SECTOR_CONCENTRATION
}
