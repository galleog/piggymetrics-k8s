package com.github.galleog.piggymetrics.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.notification.client.AccountServiceClient;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Tests for {@link NotificationService}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final int MILLIS = 100;
    private static final String ATTACHMENT = "json";

    @Mock
    private RecipientService recipientService;
    @Mock
    private AccountServiceClient client;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private NotificationService notificationService;

    private Recipient successful;
    private Recipient failed;

    @BeforeEach
    void setUp() {
        successful = Recipient.builder()
                .accountName("successful")
                .email("successful@example.com")
                .build();
        failed = Recipient.builder()
                .accountName("failed")
                .email("failed@example.com")
                .build();
        when(recipientService.readyToNotify(any(NotificationType.class))).thenReturn(ImmutableList.of(failed, successful));
    }

    /**
     * Test for {@link NotificationService#sendBackupNotifications()}.
     */
    @Test
    void shouldSendBackupNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
        when(client.getAccount(successful.getAccountName())).thenReturn(ATTACHMENT);
        when(client.getAccount(failed.getAccountName())).thenThrow(RuntimeException.class);

        notificationService.sendBackupNotifications();

        verify(emailService, timeout(MILLIS)).send(NotificationType.BACKUP, successful, ATTACHMENT);
        verify(recipientService, timeout(MILLIS)).markNotified(NotificationType.BACKUP, successful);

        verify(emailService, after(MILLIS).never()).send(eq(NotificationType.BACKUP), eq(failed), anyString());
        verify(recipientService, after(MILLIS).never()).markNotified(NotificationType.BACKUP, failed);
    }

    /**
     * Test for {@link NotificationService#sendRemindNotifications()}.
     */
    @Test
    void shouldSendRemindNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
        doThrow(RuntimeException.class).when(emailService).send(NotificationType.REMIND, failed, null);

        notificationService.sendRemindNotifications();

        verify(emailService, timeout(MILLIS)).send(NotificationType.REMIND, successful, null);
        verify(recipientService, timeout(MILLIS)).markNotified(NotificationType.REMIND, successful);

        verify(recipientService, after(MILLIS).never()).markNotified(NotificationType.REMIND, failed);
    }
}