package com.github.galleog.sk.auth.repository;

import com.github.galleog.sk.auth.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository repository;

    @Test
    public void shouldSaveAndFindUserByName() {
        User user = new User();
        user.setUsername("name");
        user.setPassword("password");
        repository.save(user);

        Optional<User> found = repository.findById(user.getUsername());
        assertThat(found.isPresent()).isTrue();
        assertThat(user.getUsername()).isEqualTo(found.get().getUsername());
        assertThat(user.getPassword()).isEqualTo(found.get().getPassword());
    }
}
