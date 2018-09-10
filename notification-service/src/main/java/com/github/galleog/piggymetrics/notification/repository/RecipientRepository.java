package com.github.galleog.piggymetrics.notification.repository;

import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Recipient}.
 */
public interface RecipientRepository extends CrudRepository<Recipient, String> {
    /**
     * Finds a recipient by their name.
     *
     * @param name the recipient's account name
     * @return the recipient with the specified account name, or {@link Optional#empty()}
     * if there is no recipient with that name
     */
    Optional<Recipient> findByAccountName(String name);

    /**
     * Finds recipients that should be notified by the specified date.
     *
     * @param type the notification type
     * @param date the date where recipients should be notified
     * @return the found recipients
     */
    @Query("select r from Recipient r join r.scheduledNotifications sn where key(sn) = ?1 and " +
            "value(sn).active = true and (value(sn).lastNotifiedDate is null or " +
            "(value(sn).lastNotifiedDate + value(sn).frequency) < ?2)")
    List<Recipient> readyToNotify(NotificationType type, LocalDate date);
}
