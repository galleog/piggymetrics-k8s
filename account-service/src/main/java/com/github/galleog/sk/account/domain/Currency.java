package com.github.galleog.sk.account.domain;

public enum Currency {

	USD, EUR, RUB;

	public static Currency getDefault() {
		return USD;
	}
}
