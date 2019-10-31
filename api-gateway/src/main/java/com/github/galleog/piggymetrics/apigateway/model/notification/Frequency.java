package com.github.galleog.piggymetrics.apigateway.model.notification;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.galleog.piggymetrics.core.enums.Enum;
import com.github.galleog.piggymetrics.core.enums.json.EnumDeserializer;

/**
 * Enumeration for notification frequencies.
 */
@JsonDeserialize(using = EnumDeserializer.class)
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
     * @param days the number of days that defines the value
     * @throws IllegalArgumentException if the enumeration contains no value with the key {@code days}
     */
    public static Frequency valueOf(int days) {
        return Enum.valueOf(Frequency.class, days);
    }
}
