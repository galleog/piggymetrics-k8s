package com.github.galleog.piggymetrics.notification.service;

import static com.github.galleog.piggymetrics.notification.domain.NotificationType.BACKUP;
import static com.github.galleog.piggymetrics.notification.domain.NotificationType.REMIND;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for {@link RecipientService}.
 */
@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String ANOTHER_EMAIL = "another@example.com";
    private static final RecipientServiceProto.GetRecipientRequest GET_RECIPIENT_REQUEST =
            RecipientServiceProto.GetRecipientRequest.newBuilder()
                    .setUserName(USERNAME)
                    .build();

    @Mock
    private RecipientRepository repository;
    private RecipientService recipientService;

    @BeforeEach
    void setUp() {
        recipientService = new RecipientService(Schedulers.immediate(), repository);
    }

    /**
     * Test for {@link RecipientService#getRecipient(Mono)}.
     */
    @Test
    void shouldGetRecipient() {
        Recipient recipient = stubRecipient();
        when(repository.getByUsername(USERNAME)).thenReturn(Optional.of(recipient));

        Mono<RecipientServiceProto.Recipient> recipientMono = recipientService.getRecipient(Mono.just(GET_RECIPIENT_REQUEST));
        StepVerifier.create(recipientMono)
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
        when(repository.getByUsername(USERNAME)).thenReturn(Optional.empty());

        Mono<RecipientServiceProto.Recipient> recipientMono = recipientService.getRecipient(Mono.just(GET_RECIPIENT_REQUEST));
        StepVerifier.create(recipientMono)
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
                .thenAnswer((Answer) invocation -> Optional.of(invocation.getArgument(0)));

        LocalDate localDate = LocalDate.now().minusDays(3);
        Date date = dateConverter().convert(localDate);
        RecipientServiceProto.NotificationSettings backup = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(false)
                .setFrequency(Frequency.MONTHLY.getKey())
                .setNotifyDate(date)
                .build();
        RecipientServiceProto.NotificationSettings remind = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.WEEKLY.getKey())
                .build();
        RecipientServiceProto.Recipient recipient = RecipientServiceProto.Recipient.newBuilder()
                .setUserName(USERNAME)
                .setEmail(ANOTHER_EMAIL)
                .putNotifications(BACKUP.name(), backup)
                .putNotifications(REMIND.name(), remind)
                .build();
        Mono<RecipientServiceProto.Recipient> recipientMono = recipientService.updateRecipient(Mono.just(recipient));
        StepVerifier.create(recipientMono)
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
    }

    /**
     * Test for {@link RecipientService#updateRecipient(Mono)} for a new recipient.
     */
    @Test
    void shouldSaveNewRecipient() {
        when(repository.update(any(Recipient.class))).thenReturn(Optional.empty());

        LocalDate localDate = LocalDate.now().minusDays(1);
        Date date = dateConverter().convert(localDate);
        RecipientServiceProto.NotificationSettings backup = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.QUARTERLY.getKey())
                .build();
        RecipientServiceProto.NotificationSettings remind = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.WEEKLY.getKey())
                .setNotifyDate(date)
                .build();
        RecipientServiceProto.Recipient recipient = RecipientServiceProto.Recipient.newBuilder()
                .setUserName(USERNAME)
                .setEmail(EMAIL)
                .putNotifications(BACKUP.name(), backup)
                .putNotifications(REMIND.name(), remind)
                .build();

        Mono<RecipientServiceProto.Recipient> recipientMono = recipientService.updateRecipient(Mono.just(recipient));
        StepVerifier.create(recipientMono)
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
        NotificationSettings backup = NotificationSettings.builder()
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