package com.github.galleog.piggymetrics.notification.domain;

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

    /**
     * Class name.
     */
    static final String CLASS_NAME = "com.github.galleog.piggymetrics.notification.domain.Frequency";

    private Frequency(int days) {
        super(days);
    }
}
