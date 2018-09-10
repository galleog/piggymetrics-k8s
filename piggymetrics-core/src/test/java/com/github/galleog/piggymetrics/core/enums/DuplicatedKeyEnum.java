package com.github.galleog.piggymetrics.core.enums;

/**
 * Enumeration type with a duplicated key.
 */
public final class DuplicatedKeyEnum extends Enum<String> {
    public static final DuplicatedKeyEnum RED = new DuplicatedKeyEnum("Red");
    public static final DuplicatedKeyEnum GREEN = new DuplicatedKeyEnum("Green");
    public static final DuplicatedKeyEnum GREENISH = new DuplicatedKeyEnum("Green");  // duplicated key

    private DuplicatedKeyEnum(String color) {
        super(color);
    }
}
