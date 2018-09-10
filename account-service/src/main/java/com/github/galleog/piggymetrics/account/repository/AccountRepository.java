package com.github.galleog.piggymetrics.account.repository;

import com.github.galleog.piggymetrics.account.domain.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Repository for {@link Account}.
 */
public interface AccountRepository extends CrudRepository<Account, String> {
    /**
     * Finds an account by its name.
     *
     * @param name the account name
     * @return the account with the specified name, or {@link Optional#empty()} if there is no account with that name
     */
    Optional<Account> findByName(String name);
}
