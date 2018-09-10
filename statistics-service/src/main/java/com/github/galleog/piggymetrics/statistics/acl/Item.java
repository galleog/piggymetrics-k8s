package com.github.galleog.piggymetrics.statistics.acl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

/**
 * Model class for an income or expense item.
 */
@Getter
@JsonDeserialize(builder = Item.ItemBuilder.class)
public class Item {
    /**
     * Item title.
     */
    @NonNull
    private String title;
    /**
     * Item monetary amount.
     */
    @NonNull
    private Money moneyAmount;
    /**
     * Item period.
     */
    @NonNull
    private TimePeriod period;

    @Builder
    private Item(@NonNull String title, @NonNull Money moneyAmount, @NonNull TimePeriod period) {
        setTitle(title);
        setMoneyAmount(moneyAmount);
        setPeriod(period);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(title).build();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class ItemBuilder {
    }
}
