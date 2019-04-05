package com.github.galleog.piggymetrics.gateway.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

/**
 * Model class for savings.
 */
@Getter
@JsonDeserialize(builder = Saving.SavingBuilder.class)
public class Saving {
    /**
     * Saving monetary amount.
     */
    @NonNull
    private Money moneyAmount;
    /**
     * Saving interest.
     */
    @NonNull
    private BigDecimal interest;
    /**
     * Indicates if the saving is a deposit.
     */
    private boolean deposit;
    /**
     * Indicates if the saving has capitalization.
     */
    private boolean capitalization;

    @Builder
    private Saving(@NonNull Money moneyAmount, @NonNull BigDecimal interest,
                   boolean deposit, boolean capitalization) {
        setMoneyAmount(moneyAmount);
        setInterest(interest);
        setDeposit(deposit);
        setCapitalization(capitalization);
    }

    private void setMoneyAmount(Money moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.isPositiveOrZero());
        this.moneyAmount = moneyAmount;
    }

    private void setInterest(BigDecimal interest) {
        Validate.notNull(interest);
        Validate.isTrue(interest.signum() >= 0);
        this.interest = interest;
    }

    private void setDeposit(boolean deposit) {
        this.deposit = deposit;
    }

    private void setCapitalization(boolean capitalization) {
        this.capitalization = capitalization;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class SavingBuilder {
    }
}
