package com.github.galleog.piggymetrics.auth.repository;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Tests for {@link UserRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer();

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
        private PasswordEncoder encoder = new BCryptPasswordEncoder();

        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(User.TABLE_NAME),
                    insertInto(User.TABLE_NAME)
                            .row()
                            .column("username", USERNAME)
                            .column("password", encoder.encode(PASSWORD))
                            .column("version", 1)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link UserRepository#findById(Object)}.
         */
        @Test
        void shouldFindUserById() {
            Optional<User> user = repository.findById(USERNAME);
            assertThat(user.isPresent()).isTrue();
            assertThat(user.get().getUsername()).isEqualTo(USERNAME);
            assertThat(encoder.matches(PASSWORD, user.get().getPassword())).isTrue();
        }
    }

    @Nested
    class WriteTest {
        private TransactionTemplate transactionTemplate;

        @BeforeEach
        void setUp() {
            transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            Operation operation = deleteAllFrom(User.TABLE_NAME);
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link UserRepository#save(Object)}.
         */
        @Test
        void shouldSaveUser() {
            User user = User.builder()
                    .username(USERNAME)
                    .password(PASSWORD)
                    .build();
            transactionTemplate.execute(status -> repository.save(user));

            Table table = new Table(dataSource, User.TABLE_NAME);
            Assertions.assertThat(table).column("username").containsValues(USERNAME);
        }
    }
}
