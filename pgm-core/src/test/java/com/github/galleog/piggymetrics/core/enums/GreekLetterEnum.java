package com.github.galleog.piggymetrics.core.enums;

import java.util.List;

/**
 * Enumeration type for Greek letters.
 */
public class GreekLetterEnum extends Enum<String> {
	public static final GreekLetterEnum ALPHA = new GreekLetterEnum("Alpha");
	public static final GreekLetterEnum BETA = new GreekLetterEnum("Beta");

	protected GreekLetterEnum(String letter) {
		super(letter);
	}

	public static GreekLetterEnum valueOf(String letter) {
		return valueOf(GreekLetterEnum.class, letter);
	}

	public static List<GreekLetterEnum> values() {
		return values(GreekLetterEnum.class);
	}
}
