package com.github.galleog.piggymetrics.auth.repository.jooq;

import static com.github.galleog.piggymetrics.auth.domain.Tables.USERS;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.domain.tables.records.UsersRecord;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of {@link UserRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqUserRepository implements UserRepository {
    private final DSLContext dsl;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getByUsername(@NonNull String username) {
        Validate.notNull(username);

        return Optional.ofNullable(dsl.select()
                .from(USERS)
                .where(USERS.USERNAME.equalIgnoreCase(username))
                .fetchOneInto(User.class));
    }

    @Override
    @Transactional
    public void save(@NonNull User user) {
        Validate.notNull(user);

        UsersRecord record = dsl.newRecord(USERS);
        record.from(user);
        record.insert();
    }
}
