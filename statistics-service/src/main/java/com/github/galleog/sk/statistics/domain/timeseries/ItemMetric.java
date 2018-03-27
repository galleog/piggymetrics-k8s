package com.github.galleog.sk.statistics.domain.timeseries;

import com.github.galleog.sk.statistics.domain.Currency;
import com.github.galleog.sk.statistics.domain.Item;
import com.github.galleog.sk.statistics.domain.TimePeriod;

import java.math.BigDecimal;

/**
 * Represents normalized {@link Item} object
 * with {@link Currency#getBase()} currency and {@link TimePeriod#getBase()} time period
 */
public class ItemMetric {

    private String title;

    private BigDecimal amount;

    public ItemMetric(String title, BigDecimal amount) {
        this.title = title;
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemMetric that = (ItemMetric) o;

        return title.equalsIgnoreCase(that.title);

    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
