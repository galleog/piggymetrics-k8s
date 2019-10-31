package com.github.galleog.piggymetrics.core.enums.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.galleog.piggymetrics.core.enums.Enum;

/**
 * Enumeration type for mathematical operations.
 */
@JsonDeserialize(using = EnumDeserializer.class)
public abstract class OperationEnum extends Enum<String> {
    public static final OperationEnum PLUS = new OperationEnum("Plus") {
        @Override
        public int eval(int a, int b) {
            return (a + b);
        }
    };

    public static final OperationEnum MINUS = new OperationEnum("Minus") {
        @Override
        public int eval(int a, int b) {
            return (a - b);
        }
    };

    protected OperationEnum(String operation) {
        super(operation);
    }

    @Override
    public Class<? extends Enum<String>> getEnumClass() {
        return OperationEnum.class;
    }

    public abstract int eval(int a, int b);
}
