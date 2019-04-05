package com.github.galleog.piggymetrics.auth.service;

import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.auth.repository.UserRepository;
import com.google.protobuf.Empty;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service to work with {@link User users}.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserService extends ReactorUserServiceGrpc.UserServiceImplBase {
    private final UserRepository repository;

    @Override
    public Mono<Empty> createUser(Mono<UserServiceProto.User> request) {
        return request.publishOn(Schedulers.elastic())
                .doOnNext(this::createUserInternal)
                .thenReturn(Empty.newBuilder().build());
    }

    @Transactional
    private void createUserInternal(UserServiceProto.User userProto) {
        User user;
        try {
            user = User.builder()
                    .username(userProto.getUserName())
                    .password(userProto.getPassword())
                    .build();
        } catch (NullPointerException | IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }

        repository.findById(user.getUsername()).ifPresent(u -> {
            throw Status.ALREADY_EXISTS
                    .withDescription("User " + user.getUsername() + " already exists")
                    .asRuntimeException();
        });
        repository.save(user);

        logger.info("New user {} has been created", user.getUsername());
    }
}
