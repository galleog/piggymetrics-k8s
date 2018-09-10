package com.github.galleog.piggymetrics.statistics.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Identifier for {@link DataPoint}.
 */
@Embeddable
@ToString
@EqualsAndHashCode
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DataPointId implements Serializable {
    @NonNull
    private String account;
    @NonNull
    private LocalDate date;

    private DataPointId(String account, LocalDate date) {
        setAccount(account);
        setDate(date);
    }

    /**
     * Creates a new instance of the identifier.
     *
     * @param account the account name
     * @param date    the date of the data point
     * @return the created instance
     * @throws NullPointerException     if the account name or date is {@code null}
     * @throws IllegalArgumentException if the account name is blank
     */
    public static DataPointId of(@NonNull String account, @NonNull LocalDate date) {
        return new DataPointId(account, date);
    }

    private void setAccount(String account) {
        Validate.notBlank(account);
        this.account = account;
    }

    private void setDate(LocalDate date) {
        Validate.notNull(date);
        this.date = date;
    }
}
