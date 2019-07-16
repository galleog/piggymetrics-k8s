package com.github.galleog.piggymetrics.statistics.domain;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;

/**
 * Entity for normalized incomes and expenses with the USD currency and
 * {@link TimePeriod#getBase()} time period.
 */
@Getter
public class ItemMetric {
    /**
     * Identifier of this item.
     */
    @Nullable
    private Long id;
    /**
     * Item type.
     */
    @NonNull
    private ItemType type;
    /**
     * Item title.
     */
    @NonNull
    private String title;
    /**
     * Metric monetary amount.
     */
    @NonNull
    private BigDecimal moneyAmount;

    @Builder
    @SuppressWarnings("unused")
    private ItemMetric(@Nullable Long id, @NonNull ItemType type, @NonNull String title, @NonNull BigDecimal moneyAmount) {
        setId(id);
        setType(type);
        setTitle(title);
        setMoneyAmount(moneyAmount);
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

    private void setMoneyAmount(BigDecimal moneyAmount) {
        Validate.notNull(moneyAmount);
        Validate.isTrue(moneyAmount.signum() == 1);
        this.moneyAmount = moneyAmount;
    }
}
