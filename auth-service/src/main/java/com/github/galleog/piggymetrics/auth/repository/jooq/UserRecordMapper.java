package com.github.galleog.piggymetrics.auth.repository.jooq;

import static com.github.galleog.piggymetrics.auth.domain.Tables.USERS;

import com.github.galleog.piggymetrics.auth.domain.User;
import org.jooq.Record;
import org.jooq.RecordMapper;

/**
 * {@link RecordMapper} for {@link User}.
 */
public class UserRecordMapper<R extends Record> implements RecordMapper<R, User> {
    @Override
    public User map(R record) {
        return User.builder()
                .username(record.get(USERS.USERNAME))
                .password(record.get(USERS.PASSWORD))
                .build();
    }
}
