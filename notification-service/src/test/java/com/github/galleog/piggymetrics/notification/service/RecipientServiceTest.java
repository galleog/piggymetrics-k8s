package com.github.galleog.piggymetrics.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for {@link RecipientService}.
 */
@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {
    private static final String ACCOUNT_NAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String ANOTHER_EMAIL = "another@example.com";

    @Mock
    private RecipientRepository repository;
    @InjectMocks
    private RecipientService recipientService;

    /**
     * Test for {@link RecipientService#findByAccountName(String)}.
     */
    @Test
    void shouldFindRecipientByAccountName() {
        Recipient recipient = stubRecipient();
        when(repository.findByAccountName(recipient.getAccountName())).thenReturn(Optional.of(recipient));
        Optional<Recipient> found = recipientService.findByAccountName(recipient.getAccountName());
        assertThat(found).containsSame(recipient);
    }

    /**
     * Test for {@link RecipientService#findByAccountName(String)} when the account name is empty.
     */
    @Test
    void shouldFailToFindRecipientWhenAccountNameIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> recipientService.findByAccountName(StringUtils.EMPTY));
    }

    /**
     * Test for {@link RecipientService#readyToNotify(NotificationType)}.
     */
    @Test
    void shouldFindReadyToNotify() {
        List<Recipient> recipients = ImmutableList.of(stubRecipient());
        when(repository.readyToNotify(eq(NotificationType.BACKUP), any(LocalDate.class))).thenReturn(recipients);

        List<Recipient> found = recipientService.readyToNotify(NotificationType.BACKUP);
        assertThat(found).containsAll(recipients);
    }

    /**
     * Test for {@link RecipientService#save(String, String, Map)} for an existing recipient.
     */
    @Test
    void shouldUpdateExistingRecipient() {
        LocalDate date = LocalDate.now().minusDays(3);
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .lastNotifiedDate(date)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .build();
        Recipient recipient = stubRecipient();
        when(repository.findByAccountName(ACCOUNT_NAME)).thenReturn(Optional.of(recipient));
        when(repository.save(recipient)).thenReturn(recipient);

        Recipient saved = recipientService.save(ACCOUNT_NAME, ANOTHER_EMAIL, ImmutableMap.of(
                NotificationType.BACKUP, backup,
                NotificationType.REMIND, remind
        ));

        assertThat(saved.getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertThat(saved.getEmail()).isEqualTo(ANOTHER_EMAIL);
        assertThat(saved.getScheduledNotifications().get(NotificationType.BACKUP)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(false, Frequency.MONTHLY, date);
        assertThat(saved.getScheduledNotifications().get(NotificationType.REMIND)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(true, Frequency.WEEKLY, null);
    }

    /**
     * Test for {@link RecipientService#save(String, String, Map)} for a new recipient.
     */
    @Test
    void shouldSaveNewRecipient() {
        LocalDate date = LocalDate.now().minusDays(1);
        NotificationSettings backup = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.QUARTERLY)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .lastNotifiedDate(date)
                .build();
        when(repository.findByAccountName(ACCOUNT_NAME)).thenReturn(Optional.empty());
        when(repository.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recipient saved = recipientService.save(ACCOUNT_NAME, EMAIL, ImmutableMap.of(
                NotificationType.BACKUP, backup,
                NotificationType.REMIND, remind
        ));

        assertThat(saved.getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getScheduledNotifications().get(NotificationType.BACKUP)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(true, Frequency.QUARTERLY, null);
        assertThat(saved.getScheduledNotifications().get(NotificationType.REMIND)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(true, Frequency.WEEKLY, date);
    }

    /**
     * Test for {@link RecipientService#markNotified(NotificationType, Recipient)}.
     */
    @Test
    void shouldMarkNotified() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .build();
        assertThat(backup.isNotified()).isFalse();
        assertThat(remind.isNotified()).isFalse();

        Recipient recipient = Recipient.builder()
                .accountName(ACCOUNT_NAME)
                .email(EMAIL)
                .scheduledNotification(NotificationType.BACKUP, backup)
                .scheduledNotification(NotificationType.REMIND, remind)
                .build();

        recipientService.markNotified(NotificationType.REMIND, recipient);

        assertThat(recipient.getScheduledNotifications().get(NotificationType.BACKUP).isNotified()).isFalse();
        assertThat(recipient.getScheduledNotifications().get(NotificationType.REMIND).isNotified()).isTrue();

        verify(repository).save(recipient);
    }

    /**
     * Test for {@link RecipientService#markNotified(NotificationType, Recipient)} if the notification being marked isn't active.
     */
    @Test
    void shouldFailToMarkNotifiedIfNotificationInactive() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .build();
        Recipient recipient = Recipient.builder()
                .accountName(ACCOUNT_NAME)
                .email(EMAIL)
                .scheduledNotification(NotificationType.BACKUP, backup)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> recipientService.markNotified(NotificationType.BACKUP, recipient));
        verify(repository, never()).save(any(Recipient.class));
    }

    /**
     * Test for {@link RecipientService#markNotified(NotificationType, Recipient)}
     * if there is no notification of the requested type.
     */
    @Test
    void shouldNotMarkNotifiedIfBackupNotificationIsNotSet() {
        Recipient recipient = stubRecipient();
        recipientService.markNotified(NotificationType.REMIND, recipient);
        assertThat(recipient.getScheduledNotifications().get(NotificationType.REMIND)).isNull();
    }

    private Recipient stubRecipient() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.QUARTERLY)
                .build();
        return Recipient.builder()
                .accountName(ACCOUNT_NAME)
                .email(EMAIL)
                .scheduledNotification(NotificationType.BACKUP, backup)
                .build();
    }
}