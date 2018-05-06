package com.github.galleog.sk8s.account.controller;

import com.github.galleog.sk8s.account.domain.Account;
import com.github.galleog.sk8s.account.domain.User;
import com.github.galleog.sk8s.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for {@link Account}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    /**
     * Returns the account by its name.
     *
     * @param name the name of the account
     * @return the found account or {@link HttpStatus#NOT_FOUND} if there is no account with the given name
     * @throws NullPointerException     if the name is {@code null}
     * @throws IllegalArgumentException if the name is blank
     */
    @NonNull
    @GetMapping("/{name}")
    @PreAuthorize("#oauth2.hasScope('server') or #name.equals('demo')")
    public ResponseEntity<Account> getAccountByName(@PathVariable @NonNull String name) {
        return accountService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets the account of the current principal.
     *
     * @param principal the current authenticated principal
     * @return the account of the current principal or {@link HttpStatus#NOT_FOUND}
     * if there is no account for the current principal
     * @throws NullPointerException if the principal is {@code null}
     */
    @NonNull
    @GetMapping("/current")
    public ResponseEntity<Account> getCurrentAccount(@NonNull Principal principal) {
        Validate.notNull(principal);
        return accountService.findByName(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the account of the current principal.
     *
     * @param principal the current authenticated principal
     * @param update    the new account data
     * @return the updated account of the current principal or {@link HttpStatus#NOT_FOUND}
     * if there is no account for the current principal
     * @throws NullPointerException if the principal or the new account data is {@code null}
     */
    @NonNull
    @PutMapping("/current")
    public ResponseEntity<?> updateCurrentAccount(@NonNull Principal principal,
                                                  @NonNull @RequestBody Account update) {
        return accountService.update(principal.getName(), update)
                .map(account -> ResponseEntity.noContent().build())
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new account with the name of the specified user and default parameters.
     *
     * @param user the user to create
     * @return the created account
     * @throws NullPointerException  if the user is {@code null}
     * @throws IllegalStateException if an account for the given user already exists
     */
    @NonNull
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createNewAccount(@NonNull @RequestBody User user) {
        return accountService.create(user);
    }

    /**
     * Returns {@link HttpStatus#BAD_REQUEST} when an exception of type {@link NullPointerException},
     * {@link IllegalArgumentException} or {@link IllegalStateException} is thrown.
     *
     * @param e the thrown exception
     */
    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void processValidationError(Exception e) {
        logger.error("HTTP 400 Bad Request is returned because invalid data are specified", e);
    }
}
