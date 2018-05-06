package com.github.galleog.sk8s.account.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Entity for incomes.
 */
@Entity
@DiscriminatorValue("i")
@JsonDeserialize(builder = Income.IncomeBuilder.class)
public class Income extends Item {
    @Builder
    private Income(@Nullable Integer id, @NonNull String title, @NonNull Money moneyAmount,
                   @NonNull TimePeriod period, @NonNull String icon) {
        super(id, title, moneyAmount, period, icon);
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class IncomeBuilder {
    }
}
