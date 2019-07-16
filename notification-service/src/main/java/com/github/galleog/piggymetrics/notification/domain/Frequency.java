package com.github.galleog.piggymetrics.notification.domain;

import com.github.galleog.piggymetrics.core.enums.Enum;

/**
 * Enumeration for notification frequencies.
 */
public class Frequency extends Enum<Integer> {
    /**
     * Notifications should be sent weekly.
     */
    public static final Frequency WEEKLY = new Frequency(7);
    /**
     * Notifications should be sent monthly.
     */
    public static final Frequency MONTHLY = new Frequency(30);
    /**
     * Notifications should be sent quarterly.
     */
    public static final Frequency QUARTERLY = new Frequency(90);

    private Frequency(int days) {
        super(days);
    }

    /**
     * Gets an enumeration value by the specified key.
     *
     * @param days       the number of days that defines the value
     * @throws IllegalArgumentException if the enumeration contains no value with the key {@code days}
     */
    public static Frequency valueOf(int days) {
        return Enum.valueOf(Frequency.class, days);
    }
}
