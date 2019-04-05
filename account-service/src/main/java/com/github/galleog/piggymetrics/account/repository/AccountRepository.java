package com.github.galleog.piggymetrics.account.repository;

import com.github.galleog.piggymetrics.account.domain.Account;
import org.springframework.lang.NonNull;

/**
 * Repository for {@link Account}.
 */
public interface AccountRepository {
    /**
     * Finds an account by its name.
     *
     * @param name the account name
     * @return the account with the specified name, or {@code null} if there is no account with that name
     */
    Account findByName(@NonNull String name);

    /**
     * Saves an account.
     *
     * @param account the account to save
     */
    void save(@NonNull Account account);
}
