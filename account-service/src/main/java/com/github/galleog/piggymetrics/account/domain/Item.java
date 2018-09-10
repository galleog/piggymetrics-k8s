package com.github.galleog.piggymetrics.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.galleog.piggymetrics.core.domain.AbstractSequencePersistable;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Columns;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity for an income or expense item.
 */
@Entity
@Table(name = Item.TABLE_NAME)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"id", "new"})
public abstract class Item extends AbstractSequencePersistable<Long> {
    @VisibleForTesting
    public static final String TABLE_NAME = "items";

    /**
     * Account the item belongs to.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    /**
     * Item title.
     */
    @Getter
    private String title;
    /**
     * Item monetary amount.
     */
    @Getter
    @Columns(columns = {@Column(name = "currency_code"), @Column(name = "amount")})
    private Money moneyAmount;
    /**
     * Item period.
     */
    @Getter
    @Enumerated(EnumType.STRING)
    private TimePeriod period;
    /**
     * Item icon.
     */
    @Getter
    private String icon;

    /**
     * Constructs a new instance of the class.
     *
     * @param title       the title of the item
     * @param moneyAmount the monetary amount of the item
     * @param period      the period the item belongs to
     * @param icon        the icon of the item
     * @throws NullPointerException     if the title, monetary amount, period or icon is {@code null}
     * @throws IllegalArgumentException if the title or the icon is blank, the title length is greater than 20,
     *                                  or if the amount is negative or equal to 0
     */
    protected Item(@NonNull String title, @NonNull Money moneyAmount, @NonNull TimePeriod period, @NonNull String icon) {
        setTitle(title);
        setMoneyAmount(moneyAmount);
        setPeriod(period);
        setIcon(icon);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(title).build();
    }

    /**
     * Sets the account for this item.
     *
     * @throws NullPointerException if the account is {@code null}
     */
    final void setAccount(@NonNull Account account) {
        Validate.notNull(account);
        this.account = account;
    }

    private void setTitle(String title) {
        Validate.notBlank(title);
        Validate.isTrue(title.length() <= 20);
        this.title = title;
    }

    private void setMoneyAmount(Money moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.isPositive());
        this.moneyAmount = moneyAmount;
    }

    private void setPeriod(TimePeriod period) {
        Validate.notNull(period);
        this.period = period;
    }

    private void setIcon(String icon) {
        Validate.notBlank(icon);
        this.icon = icon;
    }
}
