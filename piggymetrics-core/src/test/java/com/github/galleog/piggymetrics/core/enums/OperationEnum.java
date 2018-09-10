package com.github.galleog.piggymetrics.core.enums;

import java.util.List;

/**
 * Enumeration type for mathematical operations.
 */
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

    private OperationEnum(String operation) {
        super(operation);
    }

    public static OperationEnum valueOf(String operation) {
        return valueOf(OperationEnum.class, operation);
    }

    public static List<OperationEnum> values() {
        return values(OperationEnum.class);
    }

    @Override
    public Class<? extends Enum<String>> getEnumClass() {
        return OperationEnum.class;
    }

    public abstract int eval(int a, int b);
}
