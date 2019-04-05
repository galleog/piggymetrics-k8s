package com.github.galleog.piggymetrics.account.domain;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entity for accounts.
 */
@Getter
public class Account {
    /**
     * Name of the user the account belongs to.
     */
    private String name;
    /**
     * Account incomes and expenses.
     */
    private List<Item> items;
    /**
     * Account savings.
     */
    private Saving saving;
    /**
     * Date when the account was last changed.
     */
    private LocalDateTime updateTime;
    /**
     * Additional note.
     */
    private String note;

    @Builder
    @SuppressWarnings("unused")
    private Account(@NonNull String name, @NonNull @Singular Collection<Item> items,
                    @NonNull Saving saving, @Nullable String note, @Nullable LocalDateTime updateTime) {
        setName(name);
        setItems(items);
        setSaving(saving);
        setNote(note);
        setUpdateTime(updateTime == null ? LocalDateTime.now() : updateTime);
    }

    /**
     * Updates this account.
     *
     * @param items  the incomes and expenses this account should have
     * @param saving the saving this account should have
     * @param note   the optional note
     * @throws NullPointerException if the items themselves, any item they contain or the saving are {@code null}
     */
    public void update(@NonNull Collection<Item> items, @NonNull Saving saving, @Nullable String note) {
        Validate.noNullElements(items);
        Validate.notNull(saving);

        setItems(items);
        setSaving(saving);
        setNote(note);
        setUpdateTime(LocalDateTime.now());
    }

    private void setName(String name) {
        Validate.notBlank(name);
        this.name = name;
    }

    private void setItems(Collection<Item> items) {
        Validate.noNullElements(items);
        this.items = ImmutableList.copyOf(items);
    }

    private void setSaving(Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
    }

    private void setNote(String note) {
        Validate.isTrue(note == null || note.length() <= 20);
        this.note = note;
    }

    private void setUpdateTime(LocalDateTime updateTime) {
        Validate.notNull(updateTime);
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(name).build();
    }
}
