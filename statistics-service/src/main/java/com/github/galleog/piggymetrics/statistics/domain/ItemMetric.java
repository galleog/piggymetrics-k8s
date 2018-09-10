package com.github.galleog.piggymetrics.statistics.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.galleog.piggymetrics.core.domain.AbstractSequencePersistable;
import com.github.galleog.piggymetrics.statistics.acl.Item;
import com.github.galleog.piggymetrics.statistics.acl.TimePeriod;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Columns;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity for normalized {@link Item} objects with the base currency and {@link TimePeriod#getBase()} time period.
 */
@Entity
@Table(name = ItemMetric.TABLE_NAME)
@JsonIgnoreProperties({"id", "new"})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ItemMetric extends AbstractSequencePersistable<Long> {
    @VisibleForTesting
    public static final String TABLE_NAME = "item_metrics";

    /**
     * Data point this item belongs to.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({@JoinColumn(name = "account"), @JoinColumn(name = "data_point_date")})
    private DataPoint dataPoint;
    /**
     * Item type.
     */
    @Getter
    @NonNull
    @JsonIgnore
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "item_type")
    private ItemType type;
    /**
     * Item title.
     */
    @Getter
    @NonNull
    private String title;
    /**
     * Metric monetary amount.
     */
    @Getter
    @NonNull
    @Columns(columns = {@Column(name = "currency_code"), @Column(name = "amount")})
    private Money moneyAmount;

    @Builder
    @SuppressWarnings("unused")
    private ItemMetric(@NonNull ItemType type, @NonNull String title, @NonNull Money moneyAmount) {
        setType(type);
        setTitle(title);
        setMoneyAmount(moneyAmount);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(title).build();
    }

    /**
     * Sets the parent data point for this metric.
     *
     * @throws NullPointerException if the data point is {@code null}
     */
    final void setDataPoint(DataPoint dataPoint) {
        Validate.notNull(dataPoint);
        this.dataPoint = dataPoint;
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
}
