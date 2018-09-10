package com.github.galleog.piggymetrics.auth.repository;

import com.github.galleog.piggymetrics.auth.domain.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for {@link User}.
 */
public interface UserRepository extends CrudRepository<User, String> {
}
