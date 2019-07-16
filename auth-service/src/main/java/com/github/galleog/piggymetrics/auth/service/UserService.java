package com.github.galleog.piggymetrics.auth.service;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.Callable;

/**
 * Service to work with {@link User users}.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserService extends ReactorUserServiceGrpc.UserServiceImplBase {
    @VisibleForTesting
    static final String MASKED_PASSWORD = "******";

    private final Scheduler jdbcScheduler;
    private final Source source;
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    @Override
    public Mono<UserServiceProto.User> createUser(Mono<UserServiceProto.User> request) {
        return request.flatMap(user -> async(() -> createUserInternal(user)))
                .map(user ->
                        UserServiceProto.User.newBuilder()
                                .setUserName(user.getUsername())
                                .setPassword(MASKED_PASSWORD)
                                .build()
                );
    }

    @Transactional
    private User createUserInternal(UserServiceProto.User userProto) {
        User user;
        try {
            user = User.builder()
                    .username(userProto.getUserName())
                    .password(encoder.encode(userProto.getPassword()))
                    .build();
        } catch (NullPointerException | IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }

        repository.getByUsername(user.getUsername()).ifPresent(u -> {
            throw Status.ALREADY_EXISTS
                    .withDescription("User '" + user.getUsername() + "' already exists")
                    .asRuntimeException();
        });
        repository.save(user);

        // send a UserCreated event
        UserServiceProto.UserCreatedEvent event = UserServiceProto.UserCreatedEvent.newBuilder()
                .setUserName(user.getUsername())
                .build();
        source.output().send(MessageBuilder.withPayload(event).build());

        logger.info("New user '{}' created", user.getUsername());
        return user;
    }

    private <T> Mono<T> async(Callable<? extends T> supplier) {
        return Mono.<T>fromCallable(supplier)
                .subscribeOn(jdbcScheduler);
    }
}
