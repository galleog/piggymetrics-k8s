package com.github.galleog.sk8s.auth.service.security;

import com.github.galleog.sk8s.auth.domain.User;
import com.github.galleog.sk8s.auth.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JpaUserDetailsService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaUserDetailsServiceTest {
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
    public void shouldLoadByUsernameWhenUserExists() {
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
    public void shouldFailToLoadByUsernameWhenUserNotExist() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(UsernameNotFoundException.class).isThrownBy(() ->
                service.loadUserByUsername(USERNAME)
        );
    }
}