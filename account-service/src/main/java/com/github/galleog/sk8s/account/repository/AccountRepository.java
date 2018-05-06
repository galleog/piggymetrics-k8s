package com.github.galleog.sk8s.account.repository;

import com.github.galleog.sk8s.account.domain.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Account}.
 */
@Repository
public interface AccountRepository extends CrudRepository<Account, String> {
    /**
     * Finds an account by its name.
     *
     * @param name the account name
     * @return the account with the specified name, or {@link Optional#empty()} if there is no account with that name
     */
    @Nullable
    Optional<Account> findByName(@NonNull String name);
}
