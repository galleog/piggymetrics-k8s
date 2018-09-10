package com.github.galleog.piggymetrics.auth.service;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to work with {@link User users}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    /**
     * Creates a new user.
     *
     * @param user the mandatory user attributes
     * @throws NullPointerException     if the user is {@code null}
     * @throws IllegalArgumentException if a user with the same username already exists
     */
    @Transactional
    public void create(@NonNull User user) {
        Validate.notNull(user);

        repository.findById(user.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("User " + user.getUsername() + " already exists");
        });
        repository.save(user);

        logger.info("New user {} has been created", user.getUsername());
    }
}
