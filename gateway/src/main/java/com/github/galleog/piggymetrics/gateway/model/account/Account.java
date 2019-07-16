package com.github.galleog.piggymetrics.gateway.model.account;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Model class for accounts.
 */
@Getter
@JsonDeserialize(builder = Account.AccountBuilder.class)
public class Account {
    /**
     * Account name.
     */
    @Nullable
    private String name;
    /**
     * Account incomes and responses.
     */
    @NonNull
    private Set<Item> items;
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
    private LocalDateTime updateTime;

    @Builder
    private Account(@Nullable String name, @NonNull @Singular Set<Item> items, @NonNull Saving saving,
                    @Nullable String note, @Nullable LocalDateTime updateTime) {
        setName(name);
        setItems(items);
        setSaving(saving);
        setNote(note);
        setUpdateTime(updateTime);
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setItems(Set<Item> items) {
        Validate.noNullElements(items);
        this.items = ImmutableSet.copyOf(items);
    }

    private void setSaving(Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
    }

    private void setNote(String note) {
        this.note = note;
    }

    private void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class AccountBuilder {
    }
}
