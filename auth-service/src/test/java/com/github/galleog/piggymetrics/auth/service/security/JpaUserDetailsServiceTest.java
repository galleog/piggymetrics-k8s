package com.github.galleog.piggymetrics.auth.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

/**
 * Tests for {@link JpaUserDetailsService}.
 */
@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    @Mock
    private UserRepository repository;
    @InjectMocks
    private JpaUserDetailsService service;

    /**
     * Test for {@link JpaUserDetailsService#loadUserByUsername(String)} when the corresponding user exists.
     */
    @Test
    void shouldLoadByUsernameWhenUserExists() {
        final User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        when(repository.findById(USERNAME)).thenReturn(Optional.of(user));
        UserDetails actual = service.loadUserByUsername(USERNAME);
        assertThat(actual).isEqualTo(user);
    }

    /**
     * Test for {@link JpaUserDetailsService#loadUserByUsername(String)} when the corresponding user doesn't exist.
     */
    @Test
    void shouldFailToLoadByUsernameWhenUserNotExist() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(UsernameNotFoundException.class).isThrownBy(() ->
                service.loadUserByUsername(USERNAME)
        );
    }
}