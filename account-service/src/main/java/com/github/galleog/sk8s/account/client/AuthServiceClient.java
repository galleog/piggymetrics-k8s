package com.github.galleog.sk8s.account.client;

import com.github.galleog.sk8s.account.domain.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for the authentication service.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    /**
     * Creates a new user by its attributes.
     *
     * @param user the user to be created
     */
    @PostMapping(path = "/uaa/users", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    void createUser(@NonNull User user);
}
