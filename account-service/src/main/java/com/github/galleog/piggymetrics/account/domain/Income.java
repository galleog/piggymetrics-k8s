package com.github.galleog.piggymetrics.account.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Entity for incomes.
 */
@Entity
@DiscriminatorValue("i")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonDeserialize(builder = Income.IncomeBuilder.class)
public class Income extends Item {
    @Builder
    @SuppressWarnings("unused")
    private Income(@NonNull String title, @NonNull Money moneyAmount, @NonNull TimePeriod period, @NonNull String icon) {
        super(title, moneyAmount, period, icon);
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class IncomeBuilder {
    }
}
