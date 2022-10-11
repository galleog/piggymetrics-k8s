package com.github.galleog.piggymetrics.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Tests for {@link NotificationService}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final String SUCCESSFUL = "successful";
    private static final String FAILED = "failed";
    private final LocalDate date = LocalDate.now().minusDays(3);
    @Mock
    private RecipientRepository recipientRepository;
    @Mock
    private EmailService emailService;
    @Captor
    private ArgumentCaptor<AccountServiceProto.GetAccountRequest> getAccountRequestCaptor;
    @Mock
    private ReactorAccountServiceGrpc.ReactorAccountServiceStub accountServiceStub;
    private NotificationService notificationService;
    private Recipient successful;
    private Recipient failed;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(recipientRepository, emailService);
        notificationService.accountServiceStub = accountServiceStub;

        var backup = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.MONTHLY)
                .notifyDate(date)
                .build();
        var remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .notifyDate(date)
                .build();
        successful = Recipient.builder()
                .username(SUCCESSFUL)
                .email("successful@example.com")
                .notification(NotificationType.BACKUP, backup)
                .notification(NotificationType.REMIND, remind)
                .build();
        failed = Recipient.builder()
                .username(FAILED)
                .email("failed@example.com")
                .build();

        when(recipientRepository.readyToNotify(any(NotificationType.class), any(LocalDate.class)))
                .thenReturn(Flux.just(failed, successful));
        when(recipientRepository.update(any(Recipient.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    /**
     * Test for {@link NotificationService#sendBackupNotifications()}.
     */
    @Test
    void shouldSendBackupNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
        when(accountServiceStub.getAccount(getAccountRequestCaptor.capture()))
                .thenReturn(Mono.just(AccountServiceProto.Account.getDefaultInstance()));
        doThrow(RuntimeException.class).when(emailService).send(eq(NotificationType.BACKUP), eq(failed), anyString());

        notificationService.sendBackupNotifications();

        verify(emailService).send(eq(NotificationType.BACKUP), eq(successful), anyString());
        verify(recipientRepository).update(argThat(arg -> {
            assertThat(arg.getUsername()).isEqualTo(SUCCESSFUL);
            assertThat(arg.getNotifications().get(NotificationType.BACKUP).getNotifyDate()).isAfter(date);
            assertThat(arg.getNotifications().get(NotificationType.REMIND).getNotifyDate()).isEqualTo(date);
            return true;
        }));
        verify(recipientRepository, never()).update(failed);

        assertThat(getAccountRequestCaptor.getAllValues())
                .extracting(AccountServiceProto.GetAccountRequest::getName)
                .containsExactly(FAILED, SUCCESSFUL);
    }

    /**
     * Test for {@link NotificationService#sendRemindNotifications()}.
     */
    @Test
    void shouldSendRemindNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
        doThrow(RuntimeException.class).when(emailService).send(NotificationType.REMIND, failed, null);

        notificationService.sendRemindNotifications();

        verify(emailService).send(NotificationType.REMIND, successful, null);
        verify(recipientRepository).update(argThat(arg -> {
            assertThat(arg.getUsername()).isEqualTo(SUCCESSFUL);
            assertThat(arg.getNotifications().get(NotificationType.BACKUP).getNotifyDate()).isEqualTo(date);
            assertThat(arg.getNotifications().get(NotificationType.REMIND).getNotifyDate()).isAfter(date);
            return true;
        }));
        verify(recipientRepository, never()).update(failed);
    }
}