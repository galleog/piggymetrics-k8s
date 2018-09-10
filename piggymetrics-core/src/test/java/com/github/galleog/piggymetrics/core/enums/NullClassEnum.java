package com.github.galleog.piggymetrics.core.enums;

/**
 * Enumeration type with {@code null}nu {@link #getEnumClass()}.
 */
public abstract class NullClassEnum extends Enum<String> {
    public static final NullClassEnum PLUS = new NullClassEnum("Plus") {
        @Override
        public int eval(int a, int b) {
            return (a + b);
        }
    };

    public static final NullClassEnum MINUS = new NullClassEnum("Minus") {
        @Override
        public int eval(int a, int b) {
            return (a - b);
        }
    };

    private NullClassEnum(String operation) {
        super(operation);
    }

    @Override
    public Class<? extends Enum<String>> getEnumClass() {
        return null;
    }

    public abstract int eval(int a, int b);
}
