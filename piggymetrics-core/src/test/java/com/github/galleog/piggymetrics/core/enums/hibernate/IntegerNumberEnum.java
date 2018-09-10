package com.github.galleog.piggymetrics.core.enums.hibernate;

import com.github.galleog.piggymetrics.core.enums.Enum;

/**
 * Enumeration type for integers whose keys are integers.
 */
public final class IntegerNumberEnum extends Enum<Integer> {
    public static final IntegerNumberEnum ONE = new IntegerNumberEnum(1);
    public static final IntegerNumberEnum TWO = new IntegerNumberEnum(2);

    private IntegerNumberEnum(int key) {
        super(key);
    }
}
