package com.github.galleog.piggymetrics.statistics.service;

import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.money.convert.MonetaryConversions;

/**
 * Service to convert a {@link Money} amount from one currency to another.
 */
@Service
public class MonetaryConversionService {
    /**
     * Converts a {@link Money} amount from one currency to another.
     *
     * @param amount   the monetary amount to be converted
     * @param currency the currency to convert the amount to
     * @return the converted monetary amount
     * @throws NullPointerException if the amount or the currecy to convert to is {@code null}
     */
    public Money convert(Money amount, CurrencyUnit currency) {
        Validate.notNull(amount);
        Validate.notNull(currency);
        return amount.with(MonetaryConversions.getConversion(currency));
    }
}
