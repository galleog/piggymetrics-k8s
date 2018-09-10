package com.github.galleog.piggymetrics.account.acl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.piggymetrics.account.domain.Expense;
import com.github.galleog.piggymetrics.account.domain.Income;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Account DTO.
 */
@Getter
@JsonDeserialize(builder = AccountDto.AccountDtoBuilder.class)
public class AccountDto {
    /**
     * Account name.
     */
    @Nullable
    private String name;
    /**
     * Account incomes.
     */
    @NonNull
    private List<Income> incomes;
    /**
     * Account expenses.
     */
    @NonNull
    private List<Expense> expenses;
    /**
     * Account savings.
     */
    @NonNull
    private Saving saving;
    /**
     * Additional note.
     */
    @Nullable
    private String note;
    /**
     * Date when the account was last changed.
     */
    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModifiedDate;

    @Builder
    private AccountDto(@Nullable String name, @NonNull @Singular Set<Income> incomes, @NonNull @Singular Set<Expense> expenses,
                       @NonNull Saving saving, @Nullable String note, @Nullable LocalDateTime lastModifiedDate) {
        setName(name);
        setIncomes(incomes);
        setExpenses(expenses);
        setSaving(saving);
        setNote(note);
        setLastModifiedDate(lastModifiedDate);
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setIncomes(Set<Income> incomes) {
        Validate.noNullElements(incomes);
        this.incomes = ImmutableList.copyOf(incomes);
    }

    private void setExpenses(Set<Expense> expenses) {
        Validate.noNullElements(expenses);
        this.expenses = ImmutableList.copyOf(expenses);
    }

    private void setSaving(Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
    }

    private void setNote(String note) {
        this.note = note;
    }

    private void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class AccountDtoBuilder {
    }
}
