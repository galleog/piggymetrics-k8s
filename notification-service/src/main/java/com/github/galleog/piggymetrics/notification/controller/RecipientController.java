package com.github.galleog.piggymetrics.notification.controller;

import com.github.galleog.piggymetrics.notification.acl.RecipientSettings;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.service.RecipientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.security.Principal;

/**
 * REST controller for {@link Recipient}.
 */
@Slf4j
@RestController
@RequestMapping("/recipients")
@RequiredArgsConstructor
public class RecipientController {
    private final RecipientService recipientService;

    /**
     * Gets notification settings for the current principal.
     *
     * @param principal the current principal
     * @return the recipient corresponding to the current principal, or {@link HttpStatus#NOT_FOUND}
     * if there exists no recipient with the name of the current principal
     * @throws NullPointerException if the current principal is {@code null}
     */
    @GetMapping("/current")
    public ResponseEntity<Recipient> getCurrentNotificationsSettings(@NotNull Principal principal) {
        return recipientService.findByAccountName(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Saves notification settings of the current principal.
     *
     * @param principal the current principal
     * @param settings  recipient's settings to be saved
     * @return the saved recipient corresponding to the current principal
     * @throws NullPointerException if the current principal or their settings are {@code null}
     */
    @PutMapping(path = "/current", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Recipient saveCurrentNotificationsSettings(@NotNull Principal principal,
                                                      @NotNull @RequestBody RecipientSettings settings) {
        return recipientService.save(principal.getName(), settings.getEmail(), settings.getScheduledNotifications());
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
