package com.github.galleog.piggymetrics.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link Recipient}.
 */
class RecipientTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";

    /**
     * Test for {@link Recipient#markNotified(NotificationType)}.
     */
    @Test
    void shouldMarkRecipientNotified() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .build();
        Recipient recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(NotificationType.BACKUP, backup)
                .notification(NotificationType.REMIND, remind)
                .build();

        recipient.markNotified(NotificationType.REMIND);

        assertThat(recipient.getNotifications().get(NotificationType.BACKUP).isNotified()).isFalse();
        assertThat(recipient.getNotifications().get(NotificationType.REMIND).isNotified()).isTrue();
    }

    /**
     * Test for {@link Recipient#markNotified(NotificationType)} if the notification being marked isn't active.
     */
    @Test
    void shouldFailToMarkRecipientNotifiedIfNotificationInactive() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .build();
        Recipient recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(NotificationType.BACKUP, backup)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> recipient.markNotified(NotificationType.BACKUP));
    }

    /**
     * Test for {@link Recipient#markNotified(NotificationType)} if there is no notification of the requested type.
     */
    @Test
    void shouldNotMarkRecipientNotifiedIfBackupNotificationIsNotSet() {
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .build();
        Recipient recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(NotificationType.REMIND, remind)
                .build();

        recipient.markNotified(NotificationType.BACKUP);
        assertThat(recipient.getNotifications()).containsOnlyKeys(NotificationType.REMIND);
    }
}