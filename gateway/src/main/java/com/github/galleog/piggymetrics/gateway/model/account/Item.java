package com.github.galleog.piggymetrics.gateway.model.account;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.ItemType;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.TimePeriod;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

/**
 * Model class for expenses and incomes.
 */
@Getter
@JsonDeserialize(builder = Item.ItemBuilder.class)
public class Item {
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
     * Item monetary amount.
     */
    @NonNull
    private Money moneyAmount;
    /**
     * Item period.
     */
    @NonNull
    private TimePeriod period;
    /**
     * Item icon.
     */
    @NonNull
    private String icon;

    @Builder
    private Item(@NonNull ItemType type, @NonNull String title, @NonNull Money moneyAmount,
                 @NonNull TimePeriod period, @NonNull String icon) {
        setType(type);
        setTitle(title);
        setMoneyAmount(moneyAmount);
        setPeriod(period);
        setIcon(icon);
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

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class ItemBuilder {
    }
}
