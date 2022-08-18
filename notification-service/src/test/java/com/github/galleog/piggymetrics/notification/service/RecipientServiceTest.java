package com.github.galleog.piggymetrics.notification.service;

import static com.github.galleog.piggymetrics.notification.domain.NotificationType.BACKUP;
import static com.github.galleog.piggymetrics.notification.domain.NotificationType.REMIND;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.type.Date;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Map;

/**
 * Tests for {@link RecipientService}.
 */
@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String ANOTHER_EMAIL = "another@example.com";
    private static final Mono<RecipientServiceProto.GetRecipientRequest> GET_RECIPIENT_REQUEST = Mono.just(
            RecipientServiceProto.GetRecipientRequest.newBuilder()
                    .setUserName(USERNAME)
                    .build()
    );

    @Mock
    private RecipientRepository repository;
    @InjectMocks
    private RecipientService recipientService;

    /**
     * Test for {@link RecipientService#getRecipient(Mono)}.
     */
    @Test
    void shouldGetRecipient() {
        var recipient = stubRecipient();
        when(repository.getByUsername(USERNAME)).thenReturn(Mono.just(recipient));

        recipientService.getRecipient(GET_RECIPIENT_REQUEST)
                .as(StepVerifier::create)
                .expectNextMatches(r -> {
                    assertThat(r.getUserName()).isEqualTo(USERNAME);
                    assertThat(r.getEmail()).isEqualTo(EMAIL);
                    assertThat(r.getNotificationsMap()).containsOnlyKeys(BACKUP.name());
                    assertThat(r.getNotificationsMap().get(BACKUP.name())).extracting(
                            RecipientServiceProto.NotificationSettings::getActive,
                            RecipientServiceProto.NotificationSettings::getFrequency,
                            RecipientServiceProto.NotificationSettings::getNotifyDate
                    ).containsExactly(true, Frequency.QUARTERLY.getKey(), Date.getDefaultInstance());
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link RecipientService#getRecipient(Mono)} when no notifications are found.
     */
    @Test
    void shouldFailToGetRecipientWhenNoNotificationsFound() {
        when(repository.getByUsername(USERNAME)).thenReturn(Mono.empty());

        recipientService.getRecipient(GET_RECIPIENT_REQUEST)
                .as(StepVerifier::create)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    return true;
                }).verify();
    }

    /**
     * Test for {@link RecipientService#updateRecipient(Mono)} for an existing recipient.
     */
    @Test
    void shouldUpdateExistingRecipient() {
        when(repository.update(any(Recipient.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var localDate = LocalDate.now().minusDays(3);
        var date = dateConverter().convert(localDate);
        var backup = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(false)
                .setFrequency(Frequency.MONTHLY.getKey())
                .setNotifyDate(date)
                .build();
        var remind = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.WEEKLY.getKey())
                .build();
        var recipient = RecipientServiceProto.Recipient.newBuilder()
                .setUserName(USERNAME)
                .setEmail(ANOTHER_EMAIL)
                .putNotifications(BACKUP.name(), backup)
                .putNotifications(REMIND.name(), remind)
                .build();

        recipientService.updateRecipient(Mono.just(recipient))
                .as(StepVerifier::create)
                .expectNextMatches(r -> {
                    assertThat(r.getUserName()).isEqualTo(USERNAME);
                    assertThat(r.getEmail()).isEqualTo(ANOTHER_EMAIL);
                    assertThat(r.getNotificationsMap().entrySet()).extracting(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getActive(),
                            entry -> entry.getValue().getFrequency(),
                            entry -> entry.getValue().getNotifyDate()
                    ).containsOnly(
                            tuple(BACKUP.name(), false, Frequency.MONTHLY.getKey(), date),
                            tuple(REMIND.name(), true, Frequency.WEEKLY.getKey(), Date.getDefaultInstance())
                    );
                    return true;
                }).verifyComplete();

        verify(repository).update(argThat(r -> {
            assertThat(r.getUsername()).isEqualTo(USERNAME);
            assertThat(r.getEmail()).isEqualTo(ANOTHER_EMAIL);
            assertThat(r.getNotifications().entrySet()).extracting(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isActive(),
                    entry -> entry.getValue().getFrequency(),
                    entry -> entry.getValue().getNotifyDate()
            ).containsOnly(
                    tuple(BACKUP, false, Frequency.MONTHLY, localDate),
                    tuple(REMIND, true, Frequency.WEEKLY, null)
            );
            return true;
        }));
        verify(repository, never()).save(any());
    }

    /**
     * Test for {@link RecipientService#updateRecipient(Mono)} for a new recipient.
     */
    @Test
    void shouldSaveNewRecipient() {
        when(repository.update(any(Recipient.class))).thenReturn(Mono.empty());
        when(repository.save(any(Recipient.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var localDate = LocalDate.now().minusDays(1);
        var date = dateConverter().convert(localDate);
        var backup = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.QUARTERLY.getKey())
                .build();
        var remind = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.WEEKLY.getKey())
                .setNotifyDate(date)
                .build();
        var recipient = RecipientServiceProto.Recipient.newBuilder()
                .setUserName(USERNAME)
                .setEmail(EMAIL)
                .putNotifications(BACKUP.name(), backup)
                .putNotifications(REMIND.name(), remind)
                .build();

        recipientService.updateRecipient(Mono.just(recipient))
                .as(StepVerifier::create)
                .expectNextMatches(r -> {
                    assertThat(r.getUserName()).isEqualTo(USERNAME);
                    assertThat(r.getEmail()).isEqualTo(EMAIL);
                    assertThat(r.getNotificationsMap().entrySet()).extracting(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getActive(),
                            entry -> entry.getValue().getFrequency(),
                            entry -> entry.getValue().getNotifyDate()
                    ).containsOnly(
                            tuple(BACKUP.name(), true, Frequency.QUARTERLY.getKey(), Date.getDefaultInstance()),
                            tuple(REMIND.name(), true, Frequency.WEEKLY.getKey(), date)
                    );
                    return true;
                }).verifyComplete();

        verify(repository).save(argThat(r -> {
            assertThat(r.getUsername()).isEqualTo(USERNAME);
            assertThat(r.getEmail()).isEqualTo(EMAIL);
            assertThat(r.getNotifications().entrySet()).extracting(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isActive(),
                    entry -> entry.getValue().getFrequency(),
                    entry -> entry.getValue().getNotifyDate()
            ).containsOnly(
                    tuple(BACKUP, true, Frequency.QUARTERLY, null),
                    tuple(REMIND, true, Frequency.WEEKLY, localDate)
            );
            return true;
        }));
    }

    private Recipient stubRecipient() {
        var backup = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.QUARTERLY)
                .build();
        return Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .notification(BACKUP, backup)
                .build();
    }
}