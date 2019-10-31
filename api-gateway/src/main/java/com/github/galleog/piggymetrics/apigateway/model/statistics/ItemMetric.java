package com.github.galleog.piggymetrics.apigateway.model.statistics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto.ItemType;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

/**
 * Normalized incomes and expenses with the USD currency and base time period.
 */
@Getter
@JsonDeserialize(builder = ItemMetric.ItemMetricBuilder.class)
public class ItemMetric {
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
    private ItemMetric(@NonNull ItemType type, @NonNull String title, @NonNull BigDecimal moneyAmount) {
        setType(type);
        setTitle(title);
        setMoneyAmount(moneyAmount);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", getType())
                .append("title", getTitle())
                .build();
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

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class ItemMetricBuilder {
    }
}
