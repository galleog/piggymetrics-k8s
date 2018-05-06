package com.github.galleog.sk8s.auth.repository;

import com.github.galleog.sk8s.auth.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link User}.
 */
@Repository
public interface UserRepository extends CrudRepository<User, String> {
}
