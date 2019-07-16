package com.github.galleog.piggymetrics.auth.repository.jooq;

import static com.github.galleog.piggymetrics.auth.domain.Tables.USERS;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.piggymetrics.auth.AuthApplication;
import com.github.galleog.piggymetrics.auth.config.ReactorTestConfig;
import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Tests for {@link JooqUserRepository}.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        classes = {AuthApplication.class, ReactorTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class JooqUserRepositoryTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private UserRepository repository;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        destination = DataSourceDestination.with(dataSource);
    }

    @Nested
    class ReadTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(USERS.getName()),
                    insertInto(USERS.getName())
                            .row()
                            .column(USERS.USERNAME.getName(), USERNAME)
                            .column(USERS.PASSWORD.getName(), PASSWORD)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqUserRepository#getByUsername(String)}.
         */
        @Test
        void shouldGetUserByUsername() {
            Optional<User> user = repository.getByUsername(USERNAME);
            assertThat(user).isPresent();
            assertThat(user.get().getUsername()).isEqualTo(USERNAME);
            assertThat(user.get().getPassword()).isEqualTo(PASSWORD);
        }
    }

    @Nested
    class SaveTest {
        private TransactionTemplate transactionTemplate;

        @BeforeEach
        void setUp() {
            transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            Operation operation = deleteAllFrom(USERS.getName());
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqUserRepository#save(User)}.
         */
        @Test
        void shouldSaveUser() {
            User user = User.builder()
                    .username(USERNAME)
                    .password(PASSWORD)
                    .build();
            transactionTemplate.execute(status -> {
                repository.save(user);
                return null;
            });

            Table users = new Table(dataSource, USERS.getName());
            Assertions.assertThat(users)
                    .column(USERS.USERNAME.getName()).containsValues(USERNAME)
                    .column(USERS.PASSWORD.getName()).containsValues(PASSWORD);
        }
    }
}
