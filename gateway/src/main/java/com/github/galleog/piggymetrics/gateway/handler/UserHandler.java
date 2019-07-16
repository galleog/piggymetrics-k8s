package com.github.galleog.piggymetrics.gateway.handler;

import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.gateway.model.auth.User;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Routing handler for users.
 */
@Component
public class UserHandler {
    @VisibleForTesting
    static final String AUTH_SERVICE = "auth-service";

    private static final Converter<User, UserServiceProto.User> USER_CONVERTER = new UserConverter();

    @GrpcClient(AUTH_SERVICE)
    private ReactorUserServiceGrpc.ReactorUserServiceStub userServiceStub;

    /**
     * Creates a new user.
     *
     * @param request the server request
     * @return the created user
     */
    public Mono<ServerResponse> createUser(ServerRequest request) {
        Mono<User> mono = request.bodyToMono(User.class)
                .map(USER_CONVERTER::convert)
                .compose(userServiceStub::createUser)
                .map(user -> USER_CONVERTER.reverse().convert(user));
        return ServerResponse.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mono, User.class);
    }

    private static final class UserConverter extends Converter<User, UserServiceProto.User> {
        @Override
        protected UserServiceProto.User doForward(User user) {
            return UserServiceProto.User.newBuilder()
                    .setUserName(user.getUsername())
                    .setPassword(user.getPassword())
                    .build();
        }

        @Override
        protected User doBackward(UserServiceProto.User user) {
            return User.builder()
                    .username(user.getUserName())
                    .password(user.getPassword())
                    .build();
        }
    }
}
