package com.github.galleog.piggymetrics.auth.repository;

import com.github.galleog.piggymetrics.auth.domain.User;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Repository for {@link User}.
 */
public interface UserRepository {
    /**
     * Gets a user by their name.
     *
     * @param username the user's name
     * @return the user with the specified name, or {@link Optional#empty()} if there is no user with that name
     */
    Optional<User> getByUsername(@NonNull String username);

    /**
     * Saves a user.
     *
     * @param user the user to save
     */
    void save(@NonNull User user);
}
