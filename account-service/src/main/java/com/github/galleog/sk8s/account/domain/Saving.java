package com.github.galleog.sk8s.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entity for savings.
 */
@Entity
@Table(name = "savings")
@JsonDeserialize(builder = Saving.SavingBuilder.class)
public class Saving implements Persistable<Integer>, Serializable {
    @Id
    @Getter
    @JsonIgnore
    private Integer id;
    /**
     * Account the saving belongs to.
     */
    @MapsId
    @JsonIgnore
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private Account account;
    /**
     * Saving monetary amount.
     */
    @Getter
    @NonNull
    @Columns(columns = {@Column(name = "currency"), @Column(name = "amount")})
    @Type(type = "org.jadira.usertype.moneyandcurrency.moneta.PersistentMoneyAmountAndCurrency")
    private Money moneyAmount;
    /**
     * Saving interest.
     */
    @Getter
    @NonNull
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
    private Saving(@NonNull Money moneyAmount, @NonNull BigDecimal interest,
                   boolean deposit, boolean capitalization) {
        setMoneyAmount(moneyAmount);
        setInterest(interest);
        setDeposit(deposit);
        setCapitalization(capitalization);
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return this.id == null;
    }

    void setAccount(@NonNull Account account) {
        Validate.notNull(account);
        this.account = account;
    }

    private void setMoneyAmount(@NonNull Money moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.isPositiveOrZero());
        this.moneyAmount = moneyAmount;
    }

    private void setInterest(@NonNull BigDecimal interest) {
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
