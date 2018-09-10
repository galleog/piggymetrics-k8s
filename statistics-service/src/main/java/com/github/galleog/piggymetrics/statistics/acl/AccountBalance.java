package com.github.galleog.piggymetrics.statistics.acl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Model class for account balances.
 */
@Getter
@JsonDeserialize(builder = AccountBalance.AccountBalanceBuilder.class)
public class AccountBalance {
    /**
     * Account incomes.
     */
    @NonNull
    private List<Item> incomes;
    /**
     * Account expenses.
     */
    @NonNull
    private List<Item> expenses;
    /**
     * Account saving.
     */
    @NonNull
    private Saving saving;

    @Builder
    private AccountBalance(@NonNull @Singular List<Item> incomes, @NonNull @Singular List<Item> expenses, @NonNull Saving saving) {
        setIncomes(incomes);
        setExpenses(expenses);
        setSaving(saving);
    }

    private void setIncomes(List<Item> incomes) {
        Validate.noNullElements(incomes);
        this.incomes = ImmutableList.copyOf(incomes);
    }

    private void setExpenses(List<Item> expenses) {
        Validate.noNullElements(expenses);
        this.expenses = ImmutableList.copyOf(expenses);
    }

    private void setSaving(Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class AccountBalanceBuilder {
    }
}
