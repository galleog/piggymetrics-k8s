package com.github.galleog.piggymetrics.account.controller;

import com.github.galleog.piggymetrics.account.acl.AccountConverter;
import com.github.galleog.piggymetrics.account.acl.AccountDto;
import com.github.galleog.piggymetrics.account.acl.User;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.service.AccountService;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;

/**
 * REST controller for {@link Account}.
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final AccountConverter converter;

    /**
     * Returns an account by its name.
     *
     * @param name the name of the account
     * @return the found account or {@link HttpStatus#NOT_FOUND} if there is no account with the given name
     * @throws NullPointerException     if the name is {@code null}
     * @throws IllegalArgumentException if the name is blank
     */
    @GetMapping("/{name}")
    @PreAuthorize("#oauth2.hasScope('server') or #name.equals('demo')")
    public ResponseEntity<AccountDto> getAccountByName(@PathVariable @NotBlank String name) {
        return accountService.findByName(name)
                .map(account -> ResponseEntity.ok(converter.convert(account)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets the account of the current principal.
     *
     * @param principal the current authenticated principal
     * @return the account of the current principal, or {@link HttpStatus#NOT_FOUND}
     * if there is no account for the current principal
     * @throws NullPointerException if the principal is {@code null}
     */
    @GetMapping("/current")
    public ResponseEntity<AccountDto> getCurrentAccount(@NotNull Principal principal) {
        return accountService.findByName(principal.getName())
                .map(account -> ResponseEntity.ok(converter.convert(account)))
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
    @PutMapping(path = "/current", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateCurrentAccount(@NotNull Principal principal,
                                                  @NotNull @RequestBody AccountDto update) {
        List<Item> items = ImmutableList.<Item>builder()
                .addAll(update.getIncomes())
                .addAll(update.getExpenses())
                .build();
        return accountService.update(principal.getName(), items, update.getSaving(), update.getNote())
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Account createNewAccount(@NotNull @RequestBody User user) {
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
