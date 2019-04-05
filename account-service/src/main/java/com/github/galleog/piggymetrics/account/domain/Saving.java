package com.github.galleog.piggymetrics.account.domain;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

/**
 * Entity for savings.
 */
public class Saving {
    /**
     * Saving monetary amount.
     */
    @Getter
    private Money moneyAmount;
    /**
     * Saving interest.
     */
    @Getter
    private BigDecimal interest;
    /**
     * Indicates if the saving is a deposit.
     */
    @Getter
    private boolean deposit;
    /**
     * Indicates if the saving has capitalization.
     */
    @Getter
    private boolean capitalization;

    @Builder
    @SuppressWarnings("unused")
    private Saving(@NonNull Money moneyAmount, @NonNull BigDecimal interest,
                   boolean deposit, boolean capitalization) {
        setMoneyAmount(moneyAmount);
        setInterest(interest);
        setDeposit(deposit);
        setCapitalization(capitalization);
    }

//    /**
//     * Sets the account for this saving.
//     *
//     * @throws NullPointerException is the account is {@code null}
//     */
//    void setAccount(@NonNull Account account) {
//        Validate.notNull(account);
//        this.account = account;
//        this.id = account.getId();
//    }

//    /**
//     * Updates this saving using the attributes of the passed one.
//     *
//     * @param update the new saving data
//     * @throws NullPointerException if the new data are {@code null}
//     */
//    void update(@NonNull Saving update) {
//        this.setMoneyAmount(update.getMoneyAmount());
//        this.setInterest(update.getInterest());
//        this.setDeposit(update.isDeposit());
//        this.setCapitalization(update.isCapitalization());
//    }

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
}
