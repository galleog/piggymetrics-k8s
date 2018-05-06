package com.github.galleog.sk8s.auth.service;

import com.github.galleog.sk8s.auth.domain.User;
import com.github.galleog.sk8s.auth.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    @Mock
    private UserRepository repository;
    @InjectMocks
    private UserService userService;

    /**
     * Test for {@link UserService#create(User)}.
     */
    @Test
    public void shouldCreateUser() {
        final User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        userService.create(user);
        verify(repository).save(user);
    }

    /**
     * Test for {@link UserService#create(User)} when the same user already exists.
     */
    @Test
    public void shouldFailWhenUserAlreadyExists() {
        final User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        when(repository.findById(USERNAME)).thenReturn(Optional.of(user));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> userService.create(user));
    }
}
