package com.github.galleog.piggymetrics.account.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Columns;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entity for savings.
 */
@Entity
@Table(name = Saving.TABLE_NAME)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonDeserialize(builder = Saving.SavingBuilder.class)
public class Saving implements Serializable {
    @VisibleForTesting
    public static final String TABLE_NAME = "savings";

    @Id
    @Column(name = "account_id")
    private Integer id;
    /**
     * Account the saving belongs to.
     */
    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private Account account;
    /**
     * Saving monetary amount.
     */
    @Getter
    @Columns(columns = {@Column(name = "currency_code"), @Column(name = "amount")})
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

    /**
     * Sets the account for this saving.
     *
     * @throws NullPointerException is the account is {@code null}
     */
    final void setAccount(@NonNull Account account) {
        Validate.notNull(account);
        this.account = account;
        this.id = account.getId();

    }

    /**
     * Updates this saving using the attributes of the passed one.
     *
     * @param update the new saving data
     * @throws NullPointerException if the new data are {@code null}
     */
    void update(@NonNull Saving update) {
        this.setMoneyAmount(update.getMoneyAmount());
        this.setInterest(update.getInterest());
        this.setDeposit(update.isDeposit());
        this.setCapitalization(update.isCapitalization());
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
