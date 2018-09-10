package com.github.galleog.piggymetrics.statistics.acl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

/**
 * Time period values.
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public enum TimePeriod {
    YEAR(365.2425), QUARTER(91.3106), MONTH(30.4368), DAY(1), HOUR(0.0416);

    /**
     * Ratio based on the number of days in the time period.
     */
    @Getter
    @NonNull
    private final double baseRatio;

    /**
     * Gets the base time period.
     */
    @NonNull
    public static TimePeriod getBase() {
        return DAY;
    }
}
