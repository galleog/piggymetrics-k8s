package com.github.galleog.piggymetrics.account.domain;

import com.github.galleog.piggymetrics.core.domain.AbstractSequencePersistable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.NaturalId;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity for accounts.
 */
@Entity
@Table(name = Account.TABLE_NAME)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Account extends AbstractSequencePersistable<Integer> {
    @VisibleForTesting
    public static final String TABLE_NAME = "accounts";

    /**
     * Account name.
     */
    @Getter
    @NonNull
    @NaturalId
    @Column(updatable = false)
    private String name;
    /**
     * Account incomes and expenses.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private Set<Item> items = new HashSet<>();
    /**
     * Account savings.
     */
    @Getter
    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private Saving saving;
    /**
     * Date when the account was last changed.
     */
    @Getter
    @NonNull
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
    /**
     * Additional note.
     */
    @Getter
    @Nullable
    private String note;

    @Version
    @SuppressWarnings("unused")
    private Integer version;

    @Builder
    @SuppressWarnings("unused")
    private Account(@NonNull String name, @NonNull @Singular Collection<Item> items,
                    @NonNull Saving saving, @Nullable String note) {
        setName(name);
        setItems(items);
        setSaving(saving);
        setNote(note);
        setLastModifiedDate(LocalDateTime.now());
    }

    /**
     * Gets the incomes of this account.
     */
    @NonNull
    public Set<Income> getIncomes() {
        return getItems().stream().filter(item -> item instanceof Income)
                .map(item -> (Income) item)
                .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Gets the expenses of this account.
     */
    @NonNull
    public Set<Expense> getExpenses() {
        return getItems().stream().filter(item -> item instanceof Expense)
                .map(item -> (Expense) item)
                .collect(ImmutableSet.toImmutableSet());
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
        setLastModifiedDate(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(name).build();
    }

    private void setName(String name) {
        Validate.notBlank(name);
        this.name = name;
    }

    private void setItems(Collection<Item> items) {
        Validate.notNull(items);
        this.items.clear();
        items.forEach(this::addItem);
    }

    private void setSaving(Saving saving) {
        Validate.notNull(saving);
        this.saving = saving;
        this.saving.setAccount(this);
    }

    private void setNote(String note) {
        Validate.isTrue(note == null || note.length() <= 20);
        this.note = note;
    }

    private void setLastModifiedDate(LocalDateTime date) {
        Validate.notNull(date);
        this.lastModifiedDate = date;
    }

    private void addItem(Item item) {
        Validate.notNull(item);
        item.setAccount(this);
        getItems().add(item);
    }
}
