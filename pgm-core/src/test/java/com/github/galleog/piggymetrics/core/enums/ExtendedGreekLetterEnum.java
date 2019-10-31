package com.github.galleog.piggymetrics.core.enums;

/**
 * Extension of {@link GreekLetterEnum}.
 */
public final class ExtendedGreekLetterEnum extends GreekLetterEnum {
	public static final GreekLetterEnum GAMMA = new ExtendedGreekLetterEnum("Gamma");

	private ExtendedGreekLetterEnum(String letter) {
		super(letter);
	}

	public static GreekLetterEnum valueOf(String letter) {
		return valueOf(ExtendedGreekLetterEnum.class, letter);
	}
}
