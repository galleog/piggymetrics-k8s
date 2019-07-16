package com.github.galleog.piggymetrics.notification.repository;

import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Recipient}.
 */
public interface RecipientRepository {
    /**
     * Gets a recipient by their username.
     *
     * @param username the recipient username
     * @return the recipient with the specified username, or {@link Optional#empty()}
     * if there is no recipient with that username
     */
    Optional<Recipient> getByUsername(@NonNull String username);

    /**
     * Saves a recipient.
     *
     * @param recipient the recipient to save
     */
    void save(@NonNull Recipient recipient);

    /**
     * Updates a recipient.
     *
     * @param recipient the recipient to update
     * @return the updated recipient, or {@link Optional#empty()} if there is no recipient with the specified name
     */
    Optional<Recipient> update(@NonNull Recipient recipient);

    /**
     * Finds recipients that should be notified by the specified date.
     *
     * @param type the notification type
     * @param date the date where recipients should be notified
     * @return the found recipients
     */
    List<Recipient> readyToNotify(@NonNull NotificationType type, @NonNull LocalDate date);
}
