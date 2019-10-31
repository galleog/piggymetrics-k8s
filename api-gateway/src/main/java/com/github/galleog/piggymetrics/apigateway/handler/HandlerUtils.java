package com.github.galleog.piggymetrics.apigateway.handler;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * Helper for request handlers.
 */
abstract class HandlerUtils {
    @VisibleForTesting
    static final String USERNAME_CLAIM = "preferred_username";

    /**
     * Gets the name of the current user.
     *
     * @param request the server request
     */
    static Mono<String> getCurrentUser(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(authentication -> (Jwt) authentication.getPrincipal())
                .map(jwt -> jwt.getClaimAsString(USERNAME_CLAIM));
    }
}
