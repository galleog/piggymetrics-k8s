package com.github.galleog.sk8s.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.sk8s.core.domain.AbstractSequencePersistable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.*;

/**
 * Entity for an income or expense item.
 */
@Entity
@Getter
@Table(name = "items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties({"id", "new"})
public abstract class Item extends AbstractSequencePersistable<Integer> {
    /**
     * Account the item belongs to.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    /**
     * Item title.
     */
    @NonNull
    private String title;
    /**
     * Item monetary amount.
     */
    @NonNull
    @Columns(columns = {@Column(name = "currency"), @Column(name = "amount")})
    @Type(type = "org.jadira.usertype.moneyandcurrency.moneta.PersistentMoneyAmountAndCurrency")
    private Money moneyAmount;
    /**
     * Item period.
     */
    @NonNull
    @Enumerated(EnumType.STRING)
    private TimePeriod period;
    /**
     * Item icon.
     */
    @NonNull
    private String icon;

    /**
     * Constructs a new instance of the class.
     *
     * @param id          the optional identifier of the item
     * @param title       the title of the item
     * @param moneyAmount the monetary amount of the item
     * @param period      the period the item belongs to
     * @param icon        the icon of the item
     * @throws NullPointerException     if the title, monetary amount, period or icon is {@code null}
     * @throws IllegalArgumentException if the title or the icon is blank, the title length is greater than 20,
     *                                  or if the amount is negative or equal to 0
     */
    protected Item(@Nullable Integer id, @NonNull String title, @NonNull Money moneyAmount,
                   @NonNull TimePeriod period, @NonNull String icon) {
        setId(id);
        setTitle(title);
        setMoneyAmount(moneyAmount);
        setPeriod(period);
        setIcon(icon);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(title).build();
    }

    private void setTitle(@NonNull String title) {
        Validate.notBlank(title);
        Validate.isTrue(title.length() <= 20);
        this.title = title;
    }

    private void setMoneyAmount(@NonNull Money moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.isPositive());
        this.moneyAmount = moneyAmount;
    }

    private void setPeriod(@NonNull TimePeriod period) {
        Validate.notNull(period);
        this.period = period;
    }

    private void setIcon(@NonNull String icon) {
        Validate.notBlank(icon);
        this.icon = icon;
    }
}
