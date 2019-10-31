package com.github.galleog.piggymetrics.core.enums;

/**
 * Enumeration type with invalid {@link #getEnumClass()}.
 */
public abstract class InvalidClassEnum extends Enum<String> {
	public static final InvalidClassEnum PLUS = new InvalidClassEnum("Plus") {
		@Override
		public int eval(int a, int b) {
			return (a + b);
		}
	};

	public static final InvalidClassEnum MINUS = new InvalidClassEnum("Minus") {
		@Override
		public int eval(int a, int b) {
			return (a - b);
		}
	};

	private InvalidClassEnum(String operation) {
		super(operation);
	}

	@Override
	public Class<? extends Enum<String>> getEnumClass() {
		return ColorEnum.class;
	}

	public abstract int eval(int a, int b);
}
