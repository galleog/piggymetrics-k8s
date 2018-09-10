package com.github.galleog.piggymetrics.statistics.acl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

/**
 * Model class for the nested saving.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Saving {
    @NonNull
    private Money moneyAmount;

    private Saving(Money moneyAmount) {
        setMoneyAmount(moneyAmount);
    }

    /**
     * Creates a new instance of the class.
     *
     * @param moneyAmount the monetary amount of the saving
     * @return the created instance
     * @throws NullPointerException     if the amount is {@code null}
     * @throws IllegalArgumentException if the amount is negative
     */
    @NonNull
    @JsonCreator
    public static Saving of(@JsonProperty("moneyAmount") @NonNull Money moneyAmount) {
        return new Saving(moneyAmount);
    }

    private void setMoneyAmount(Money moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.isPositiveOrZero());
        this.moneyAmount = moneyAmount;
    }
}
