package com.github.galleog.piggymetrics.notification.repository;

import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Repository for {@link Recipient}.
 */
public interface RecipientRepository {
    /**
     * Gets a recipient by their username.
     *
     * @param username the recipient username
     * @return the recipient with the specified username
     */
    Mono<Recipient> getByUsername(@NonNull String username);

    /**
     * Saves a recipient.
     *
     * @param recipient the recipient to save
     * @return the saved recipient
     */
    Mono<Recipient> save(@NonNull Recipient recipient);

    /**
     * Updates a recipient.
     *
     * @param recipient the recipient to update
     * @return the updated recipient
     */
    Mono<Recipient> update(@NonNull Recipient recipient);

    /**
     * Finds recipients that should be notified by the specified date.
     *
     * @param type the notification type
     * @param date the date where recipients should be notified
     * @return the found recipients
     */
    Flux<Recipient> readyToNotify(@NonNull NotificationType type, @NonNull LocalDate date);
}
