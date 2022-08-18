package com.github.galleog.piggymetrics.notification.domain;

import static com.github.galleog.piggymetrics.notification.domain.Frequency.MONTHLY;
import static com.github.galleog.piggymetrics.notification.domain.Frequency.WEEKLY;
import static com.github.galleog.piggymetrics.notification.domain.NotificationType.BACKUP;
import static com.github.galleog.piggymetrics.notification.domain.NotificationType.REMIND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests for {@link Recipient}.
 */
class RecipientTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";

    /**
     * Test for {@link Recipient#markNotified(NotificationType)}.
     */
    @Test
    void shouldMarkRecipientNotified() {
        var backup = NotificationSettings.builder()
                .active(false)
                .frequency(MONTHLY)
                .build();
        var remind = NotificationSettings.builder()
                .active(true)
                .frequency(WEEKLY)
                .build();
        var recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(BACKUP, backup)
                .notification(REMIND, remind)
                .build();

        var notified = recipient.markNotified(REMIND);

        assertThat(notified.getUsername()).isEqualTo(USERNAME);
        assertThat(notified.getEmail()).isEqualTo(EMAIL);
        assertThat(notified.getNotifications().entrySet()).extracting(
                Map.Entry::getKey,
                entry -> entry.getValue().isActive(),
                entry -> entry.getValue().getFrequency(),
                entry -> entry.getValue().isNotified()
        ).containsExactlyInAnyOrder(
                tuple(BACKUP, false, MONTHLY, false),
                tuple(REMIND, true, WEEKLY, true)
        );
    }

    /**
     * Test for {@link Recipient#markNotified(NotificationType)} if the notification being marked isn't active.
     */
    @Test
    void shouldFailToMarkRecipientNotifiedIfNotificationInactive() {
        var backup = NotificationSettings.builder()
                .active(false)
                .frequency(MONTHLY)
                .build();
        var recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(BACKUP, backup)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> recipient.markNotified(BACKUP));
    }

    /**
     * Test for {@link Recipient#markNotified(NotificationType)} if there is no notification of the requested type.
     */
    @Test
    void shouldNotMarkRecipientNotifiedIfBackupNotificationIsNotSet() {
        var remind = NotificationSettings.builder()
                .active(true)
                .frequency(WEEKLY)
                .build();
        var recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(REMIND, remind)
                .build();

        var notified = recipient.markNotified(BACKUP);

        assertThat(notified.getUsername()).isEqualTo(USERNAME);
        assertThat(notified.getEmail()).isEqualTo(EMAIL);
        assertThat(notified.getNotifications().entrySet()).extracting(
                Map.Entry::getKey,
                entry -> entry.getValue().isActive(),
                entry -> entry.getValue().getFrequency(),
                entry -> entry.getValue().isNotified()
        ).containsExactly(
                tuple(REMIND, true, WEEKLY, false)
        );
    }
}