package com.github.galleog.sk8s.auth.controller;

import com.github.galleog.sk8s.auth.domain.User;
import com.github.galleog.sk8s.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for {@link User}.
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Returns the current principal.
     */
    @NonNull
    @GetMapping("/current")
    public Principal currentPrincipal(@NonNull Principal principal) {
        return principal;
    }

    /**
     * Creates a new user by its attributes.
     *
     * @param user the user to be created
     * @throws NullPointerException     if the user is {@code null}
     * @throws IllegalArgumentException if a user with the same username already exists
     */
    @PostMapping
    @PreAuthorize("#oauth2.hasScope('server')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@NonNull @RequestBody User user) {
        userService.create(user);
    }

    /**
     * Returns {@link HttpStatus#BAD_REQUEST} when an exception of type {@link NullPointerException} or
     * {@link IllegalArgumentException} is thrown.
     *
     * @param e the thrown exception
     */
    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void processValidationError(Exception e) {
        logger.warn("HTTP 400 Bad Request is returned because invalid data are specified", e);
    }
}
