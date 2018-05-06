package com.github.galleog.sk8s.account.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableSet;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable value object for account balances.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonDeserialize(builder = AccountBalance.AccountBalanceBuilder.class)
public class AccountBalance implements Serializable {
    /**
     * Account incomes and expenses.
     */
    @NonNull
    @OneToMany(orphanRemoval = true, mappedBy = "account")
    private Set<Item> items;
    /**
     * Account savings.
     */
    @Getter
    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private Saving saving;

    @Builder
    private AccountBalance(@Nullable @Singular Set<Item> incomes, @Nullable @Singular Set<Item> expenses,
                           @NonNull Saving saving) {
        setIncomes(incomes);
        setExpenses(expenses);
        setSaving(saving);
    }

    /**
     * Gets the incomes of this account.
     */
    public Set<Item> getIncomes() {
        return this.items.stream().filter(item -> item instanceof Income)
                .map(item -> (Income) item)
                .collect(ImmutableSet.toImmutableSet());
    }

    private void setIncomes(@Nullable Set<Item> incomes) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        if (incomes != null) {
            this.items.addAll(incomes);
        }
    }

    /**
     * Gets the expenses of this account.
     */
    public Set<Item> getExpenses() {
        return this.items.stream().filter(item -> item instanceof Expense)
                .collect(ImmutableSet.toImmutableSet());
    }

    private void setExpenses(@Nullable Set<Item> expenses) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        if (expenses != null) {
            this.items.addAll(expenses);
        }
    }

    private void setSaving(@NonNull Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class AccountBalanceBuilder {
    }
}
