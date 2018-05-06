package com.github.galleog.sk8s.auth.repository;

import com.github.galleog.sk8s.auth.domain.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.util.Optional;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserRepository}.
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    private static final DbSetupTracker dbSetupTracker = new DbSetupTracker();

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserRepository repository;
    private PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Before
    public void setUp() {
        Operation operation = deleteAllFrom("users");
        DbSetup dbSetup = new DbSetup(DataSourceDestination.with(dataSource), operation);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    /**
     * Test for {@link UserRepository#save(Object)} and {@link UserRepository#findById(Object)}.
     */
    @Test
    public void shouldSaveUserAndFindItById() {
        final User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
        repository.save(user);

        Optional<User> found = repository.findById(USERNAME);
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().getUsername()).isEqualTo(USERNAME);
        assertThat(encoder.matches(PASSWORD, found.get().getPassword()));
    }
}
