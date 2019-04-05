package com.github.galleog.piggymetrics.auth.service;

import static com.github.galleog.piggymetrics.auth.domain.User.ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
     * Test for {@link UserService#createUser(Mono)}.
     */
    @Test
    void shouldCreateUser() {
        when(repository.findById(USERNAME)).thenReturn(Optional.empty());

        Mono<Empty> mono = userService.createUser(Mono.just(stubUserProto()));
        StepVerifier.create(mono)
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).save(argThat(arg -> {
            assertThat(arg.getUsername()).isEqualTo(USERNAME);
            assertThat(ENCODER.matches(PASSWORD, arg.getPassword())).isTrue();
            return true;
        }));
    }

    /**
     * Test for {@link UserService#createUser(Mono)} when the same user already exists.
     */
    @Test
    void shouldFailWhenUserAlreadyExists() {
        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        when(repository.findById(USERNAME)).thenReturn(Optional.of(user));

        Mono<Empty> mono = userService.createUser(Mono.just(stubUserProto()));
        StepVerifier.create(mono)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
                    return true;
                }).verify();

        verify(repository, never()).save(any(User.class));
    }

    /**
     * Test for {@link UserService#createUser(Mono)} when user data are invalid
     */
    @Test
    void shouldFailWhenUserNameIsEmpty() {
        UserServiceProto.User user = UserServiceProto.User.newBuilder()
                .setPassword(PASSWORD)
                .build();
        Mono<Empty> mono = userService.createUser(Mono.just(user));
        StepVerifier.create(mono)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    return true;
                }).verify();
    }

    private UserServiceProto.User stubUserProto() {
        return UserServiceProto.User.newBuilder()
                .setUserName(USERNAME)
                .setPassword(PASSWORD)
                .build();
    }
}
