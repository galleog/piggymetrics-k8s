package com.github.galleog.piggymetrics.account.repository;

import com.github.galleog.piggymetrics.account.domain.Account;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Account}.
 */
public interface AccountRepository {
    /**
     * Gets an account by its name.
     *
     * @param name the account name
     * @return the account with the specified name
     */
    Mono<Account> getByName(@NonNull String name);

    /**
     * Saves an account.
     *
     * @param account the account to save
     * @return the saved account
     */
    Mono<Account> save(@NonNull Account account);

    /**
     * Updates an account.
     *
     * @param account the account to update
     * @return the updated account
     */
    Mono<Account> update(@NonNull Account account);
}
