package com.github.galleog.piggymetrics.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.collect.ImmutableList;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDate;

/**
 * Tests for {@link NotificationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {
    private static final String SUCCESSFUL = "successful";
    private static final String FAILED = "failed";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private RecipientRepository recipientRepository;
    @Mock
    private EmailService emailService;
    @Captor
    private ArgumentCaptor<Mono<AccountServiceProto.GetAccountRequest>> getAccountRequestCaptor;

    private ReactorAccountServiceGrpc.AccountServiceImplBase accountService;
    private NotificationService notificationService;

    private LocalDate date = LocalDate.now().minusDays(3);
    private Recipient successful;
    private Recipient failed;

    @Before
    public void setUp() throws Exception {
        accountService = spy(new ReactorAccountServiceGrpc.AccountServiceImplBase() {
        });

        grpcCleanup.register(InProcessServerBuilder.forName(NotificationService.ACCOUNT_SERVICE)
                .directExecutor()
                .addService(accountService)
                .build()
                .start());
        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(NotificationService.ACCOUNT_SERVICE)
                .directExecutor()
                .build());

        notificationService = new NotificationService(Schedulers.immediate(), recipientRepository, emailService);
        notificationService.accountServiceStub = ReactorAccountServiceGrpc.newReactorStub(channel);

        NotificationSettings backup = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.MONTHLY)
                .notifyDate(date)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
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
                .thenReturn(ImmutableList.of(failed, successful));
    }

    /**
     * Test for {@link NotificationService#sendBackupNotifications()}.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSendBackupNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
        doReturn(Mono.just(AccountServiceProto.Account.getDefaultInstance())).when(accountService)
                .getAccount(getAccountRequestCaptor.capture());
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

        StepVerifier.create(getAccountRequestCaptor.getAllValues().get(0))
                .expectNextMatches(request -> {
                    assertThat(request.getName()).isEqualTo(FAILED);
                    return true;
                }).verifyComplete();
        StepVerifier.create(getAccountRequestCaptor.getAllValues().get(1))
                .expectNextMatches(request -> {
                    assertThat(request.getName()).isEqualTo(SUCCESSFUL);
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link NotificationService#sendRemindNotifications()}.
     */
    @Test
    public void shouldSendRemindNotificationsEvenWhenErrorsOccurForSomeRecipients() throws Exception {
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