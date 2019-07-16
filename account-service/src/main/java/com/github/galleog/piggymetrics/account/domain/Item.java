package com.github.galleog.piggymetrics.account.domain;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Entity for an income or expense item.
 */
@Getter
public class Item {
    /**
     * Identifier of the item.
     */
    private Long id;
    /**
     * Item type.
     */
    private ItemType type;
    /**
     * Item title.
     */
    private String title;
    /**
     * Item monetary amount.
     */
    private Money moneyAmount;
    /**
     * Item period.
     */
    private TimePeriod period;
    /**
     * Item icon.
     */
    private String icon;

    @Builder
    private Item(@Nullable Long id, @NonNull ItemType type, @NonNull String title, @NonNull Money moneyAmount,
                 @NonNull TimePeriod period, @NonNull String icon) {
        setId(id);
        setType(type);
        setTitle(title);
        setMoneyAmount(moneyAmount);
        setPeriod(period);
        setIcon(icon);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("type", getType())
                .append("title", getTitle())
                .build();
    }

    private void setId(Long id) {
        this.id = id;
    }

    private void setType(ItemType type) {
        Validate.notNull(type);
        this.type = type;
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
