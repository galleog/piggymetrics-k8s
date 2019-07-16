package com.github.galleog.piggymetrics.account.repository;

import com.github.galleog.piggymetrics.account.domain.Account;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Repository for {@link Account}.
 */
public interface AccountRepository {
    /**
     * Gets an account by its name.
     *
     * @param name the account name
     * @return the account with the specified name, or {@link Optional#empty()} if there is no account with that name
     */
    Optional<Account> getByName(@NonNull String name);

    /**
     * Saves an account.
     *
     * @param account the account to save
     * @return the saved account
     */
    @NonNull
    Account save(@NonNull Account account);

    /**
     * Updates an account.
     *
     * @param account the account to update
     * @return the updated account, or {@link Optional#empty()} if there is no account with the specified name
     */
    Optional<Account> update(@NonNull Account account);
}
