package com.github.galleog.piggymetrics.notification.service;

import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for {@link Recipient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipientService {
    private final RecipientRepository recipientRepository;

    /**
     * Finds a recipient by their name.
     *
     * @param name recipient's account name
     * @return the recipient with the specified account name, or {@link Optional#empty()}
     * if there is no recipient with that name
     * @throws NullPointerException     if the account name is {@code null}
     * @throws IllegalArgumentException if the account name is blank
     */
    public Optional<Recipient> findByAccountName(@NonNull String name) {
        Validate.notBlank(name);
        return recipientRepository.findByAccountName(name);
    }

    /**
     * Finds recipients that should be notified shortly.
     *
     * @param type the notification type
     * @return the found recipients
     * @throws NullPointerException if the type is {@code null}
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public List<Recipient> readyToNotify(@NonNull NotificationType type) {
        Validate.notNull(type);
        return recipientRepository.readyToNotify(type, LocalDate.now());
    }

    /**
     * Creates or updates recipient settings
     *
     * @param accountName recipient's account name
     * @param email       the new recipient email
     * @param settings    the new notification settings
     * @return the updated recipient
     * @throws NullPointerException     if the account name or settings are {@code null}
     * @throws IllegalArgumentException if the account name is blank or the email is invalid
     */
    @NonNull
    @Transactional
    public Recipient save(@NonNull String accountName, @NonNull String email,
                          @NonNull Map<NotificationType, NotificationSettings> settings) {
        Validate.notBlank(accountName);
        Validate.isTrue(EmailValidator.getInstance().isValid(email));
        Validate.notNull(settings);

        Recipient recipient;
        Optional<Recipient> optional = recipientRepository.findByAccountName(accountName);
        if (optional.isPresent()) {
            logger.debug("Notification settings for the recipient {} will be updated", accountName);

            recipient = optional.get();
            recipient.updateSettings(email, settings);
        } else {
            recipient = Recipient.builder()
                    .accountName(accountName)
                    .email(email)
                    .scheduledNotifications(settings)
                    .build();
        }
        Recipient saved = recipientRepository.save(recipient);

        logger.info("Notification settings of the recipient {} saved", saved);
        return saved;
    }

    /**
     * Updates the last notified date of the specified recipient.
     *
     * @param type      the notification type
     * @param recipient the recipient to be updated
     * @throws NullPointerException     if the notification type or recipient is {@code null}
     * @throws IllegalArgumentException if recipient's notification settings of the specified type aren't active
     */
    public void markNotified(@NonNull NotificationType type, @NonNull Recipient recipient) {
        Validate.notNull(recipient);
        recipient.markNotified(type);
        recipientRepository.save(recipient);
    }
}
