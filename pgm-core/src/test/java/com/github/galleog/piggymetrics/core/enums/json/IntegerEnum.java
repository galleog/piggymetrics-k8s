package com.github.galleog.piggymetrics.core.enums.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.galleog.piggymetrics.core.enums.Enum;

/**
 * Enumeration type for integers.
 */
@JsonDeserialize(using = EnumDeserializer.class)
public class IntegerEnum extends Enum<Integer> {
    public static final IntegerEnum ONE = new IntegerEnum(1);
    public static final IntegerEnum TWO = new IntegerEnum(2);

    protected IntegerEnum(int key) {
        super(key);
    }
}
