package com.github.galleog.sk8s.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.sk8s.core.domain.AbstractSequencePersistable;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for accounts.
 */
@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = {"id", "new", "version"})
@JsonDeserialize(builder = Account.AccountBuilder.class)
public class Account extends AbstractSequencePersistable<Integer> {
    /**
     * Account name.
     */
    @Getter
    @NonNull
    @NaturalId
    @Column(updatable = false)
    private String name;
    /**
     * Date when the account was last changed.
     */
    @Getter
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
    /**
     * Account balance.
     */
    @Getter
    @NonNull
    @Embedded
    private AccountBalance balance;
    /**
     * Additional note.
     */
    @Getter
    @Nullable
    private String note;

    @Version
    @Nullable
    private Integer version;

    @Builder
    private Account(@Nullable Integer id, @NonNull String name,
                    @NonNull AccountBalance balance, @Nullable String note) {
        setId(id);
        setName(name);
        setBalance(balance);
        setNote(note);
    }

    /**
     * Updates this account using attributes of the passed one.
     *
     * @param update the new account data
     */
    public void update(@NonNull Account update) {
        setBalance(update.getBalance());
        setNote(update.getNote());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(name).build();
    }


    private void setName(@NonNull String name) {
        Validate.notBlank(name);
        this.name = name;
    }

    private void setBalance(@NonNull AccountBalance balance) {
        Validate.notNull(balance);
        this.balance = balance;
    }

    private void setNote(@Nullable String note) {
        Validate.isTrue(note == null || note.length() <= 20);
        this.note = note;
    }

    @PreUpdate
    @PrePersist
    private void setAccountToSaving() {
        getBalance().getSaving().setAccount(this);
    }

    @JsonIgnoreProperties("lastModifiedDate")
    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class AccountBuilder {
    }
}
