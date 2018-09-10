package com.github.galleog.piggymetrics.auth.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

/**
 * Tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
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
    void shouldCreateUser() {
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
    void shouldFailWhenUserAlreadyExists() {
        final User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        when(repository.findById(USERNAME)).thenReturn(Optional.of(user));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> userService.create(user));
    }
}
