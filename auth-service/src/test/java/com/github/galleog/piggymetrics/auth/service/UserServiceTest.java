package com.github.galleog.piggymetrics.auth.service;

import static com.github.galleog.piggymetrics.auth.service.UserService.MASKED_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.AuthApplication;
import com.github.galleog.piggymetrics.auth.config.GrpcTestConfig;
import com.github.galleog.piggymetrics.auth.config.ReactorTestConfig;
import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/**
 * Tests for {@link UserService}.
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {AuthApplication.class, UserServiceTest.Config.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class UserServiceTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @MockBean
    private UserRepository repository;
    @MockBean
    private PasswordEncoder encoder;
    @Autowired
    private Source source;
    @Autowired
    private MessageCollector collector;
    private ReactorUserServiceGrpc.ReactorUserServiceStub userServiceStub;
    private BlockingQueue<Message<?>> messages;

    @Before
    public void setUp() {
        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(GrpcTestConfig.SERVICE_NAME)
                .directExecutor()
                .build());
        userServiceStub = ReactorUserServiceGrpc.newReactorStub(channel);

        messages = collector.forChannel(source.output());
        messages.poll();
    }

    /**
     * Test for {@link UserService#createUser(Mono)}.
     */
    @Test
    public void shouldCreateUser() {
        BlockingQueue<Message<?>> messages = collector.forChannel(source.output());

        when(encoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(repository.getByUsername(USERNAME)).thenReturn(Optional.empty());

        StepVerifier.create(userServiceStub.createUser(stubUserProto()))
                .expectNextMatches(user -> {
                    assertThat(user.getUserName()).isEqualTo(USERNAME);
                    assertThat(user.getPassword()).isEqualTo(MASKED_PASSWORD);
                    return true;
                }).verifyComplete();

        verify(encoder).encode(PASSWORD);
        verify(repository).save(argThat(arg -> {
            assertThat(arg.getUsername()).isEqualTo(USERNAME);
            assertThat(arg.getPassword()).isEqualTo(PASSWORD);
            return true;
        }));

        assertThat(messages).extracting(msg -> (byte[]) msg.getPayload())
                .containsExactly(
                        UserServiceProto.UserCreatedEvent.newBuilder()
                                .setUserName(USERNAME)
                                .build()
                                .toByteArray()
                );
    }

    /**
     * Test for {@link UserService#createUser(Mono)} when the same user already exists.
     */
    @Test
    public void shouldFailToCreateUserWhenUserAlreadyExists() {
        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        when(encoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(repository.getByUsername(USERNAME)).thenReturn(Optional.of(user));

        StepVerifier.create(userServiceStub.createUser(stubUserProto()))
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
                    return true;
                }).verify();

        verify(repository, never()).save(any(User.class));
        assertThat(messages).isEmpty();
    }

    /**
     * Test for {@link UserService#createUser(Mono)} when user data are invalid
     */
    @Test
    public void shouldFailToCreateUserWhenUserNameIsEmpty() {
        UserServiceProto.User user = UserServiceProto.User.newBuilder()
                .setPassword(PASSWORD)
                .build();
        StepVerifier.create(userServiceStub.createUser(user))
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    return true;
                }).verify();

        assertThat(messages).isEmpty();
    }

    private UserServiceProto.User stubUserProto() {
        return UserServiceProto.User.newBuilder()
                .setUserName(USERNAME)
                .setPassword(PASSWORD)
                .build();
    }

    @Configuration
    @Import({GrpcTestConfig.class, ReactorTestConfig.class})
    @ImportAutoConfiguration(exclude = {JooqAutoConfiguration.class, LiquibaseAutoConfiguration.class})
    static class Config {
    }
}
