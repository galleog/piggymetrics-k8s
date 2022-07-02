package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.model.notification.NotificationType.BACKUP;
import static com.github.galleog.piggymetrics.apigateway.model.notification.NotificationType.REMIND;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.github.galleog.piggymetrics.apigateway.model.notification.Frequency;
import com.github.galleog.piggymetrics.apigateway.model.notification.NotificationSettings;
import com.github.galleog.piggymetrics.apigateway.model.notification.Recipient;
import com.github.galleog.piggymetrics.notification.grpc.ReactorRecipientServiceGrpc.RecipientServiceImplBase;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto;
import com.google.common.collect.ImmutableMap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Map;

/**
 * Tests for routing notification requests.
 */
@RunWith(SpringRunner.class)
public class NotificationRequestRouterTest extends BaseRouterTest {
    private static final String USERNAME = "test";
    private static final String URI = "/notifications/current";
    private static final String EMAIL = "test@example.com";
    private static final LocalDate BACKUP_DATE = LocalDate.now().minusDays(10);
    private static final LocalDate REMIND_DATE = LocalDate.now().minusDays(3);

    @Captor
    private ArgumentCaptor<Mono<RecipientServiceProto.GetRecipientRequest>> requestCaptor;
    @Captor
    private ArgumentCaptor<Mono<RecipientServiceProto.Recipient>> recipientCaptor;

    private RecipientServiceImplBase recipientService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        recipientService = spyGrpcService(RecipientServiceImplBase.class, NotificationHandler.NOTIFICATION_SERVICE);
    }

    /**
     * Test for GET /recipients/current.
     */
    @Test
    public void shouldGetCurrentRecipientSettings() {
        doReturn(Mono.just(stubRecipientProto())).when(recipientService).getRecipient(requestCaptor.capture());

        webClient.mutateWith(mockJwt(USERNAME))
                .get()
                .uri(URI)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Recipient.class)
                .value(recipient -> {
                    assertThat(recipient.getUsername()).isEqualTo(USERNAME);
                    assertThat(recipient.getEmail()).isEqualTo(EMAIL);
                    assertThat(recipient.getNotifications().entrySet()).extracting(
                            Map.Entry::getKey,
                            entry -> entry.getValue().isActive(),
                            entry -> entry.getValue().getFrequency(),
                            entry -> entry.getValue().getNotifyDate()
                    ).containsOnly(
                            tuple(REMIND, true, Frequency.WEEKLY, REMIND_DATE),
                            tuple(BACKUP, false, Frequency.MONTHLY, BACKUP_DATE)
                    );
                });

        StepVerifier.create(requestCaptor.getValue())
                .expectNextMatches(req -> USERNAME.equals(req.getUserName()))
                .verifyComplete();
    }

    /**
     * Test for GET /recipients/current when there is no recipient with the name of the current principal.
     */
    @Test
    public void shouldFailToGeCurrentRecipientSettingsIfRecipientDoesNotExist() {
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(recipientService).getRecipient(requestCaptor.capture());

        webClient.mutateWith(mockJwt(USERNAME))
                .get()
                .uri(URI)
                .exchange()
                .expectStatus().isNotFound();

        StepVerifier.create(requestCaptor.getValue())
                .expectNextMatches(req -> USERNAME.equals(req.getUserName()))
                .verifyComplete();
    }

    /**
     * Test for GET /recipients/current without authentication.
     */
    @Test
    public void shouldFailToGetCurrentRecipientSettingsWithoutAuthentication() {
        webClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test for PUT /recipients/current.
     */
    @Test
    public void shouldUpdateCurrentRecipientSettings() {
        doReturn(Mono.just(stubRecipientProto())).when(recipientService).updateRecipient(recipientCaptor.capture());

        webClient.mutateWith(mockJwt(USERNAME))
                .mutateWith(csrf())
                .put()
                .uri(URI)
                .bodyValue(stubRecipient())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Recipient.class)
                .value(recipient -> {
                    assertThat(recipient.getUsername()).isEqualTo(USERNAME);
                    assertThat(recipient.getEmail()).isEqualTo(EMAIL);
                    assertThat(recipient.getNotifications().entrySet()).extracting(
                            Map.Entry::getKey,
                            entry -> entry.getValue().isActive(),
                            entry -> entry.getValue().getFrequency(),
                            entry -> entry.getValue().getNotifyDate()
                    ).containsOnly(
                            tuple(REMIND, true, Frequency.WEEKLY, REMIND_DATE),
                            tuple(BACKUP, false, Frequency.MONTHLY, BACKUP_DATE)
                    );
                });

        StepVerifier.create(recipientCaptor.getValue())
                .expectNextMatches(recipient -> {
                    assertThat(recipient.getUserName()).isEqualTo(USERNAME);
                    assertThat(recipient.getEmail()).isEqualTo(EMAIL);
                    assertThat(recipient.getNotificationsMap().entrySet()).extracting(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getActive(),
                            entry -> entry.getValue().getFrequency(),
                            entry -> entry.getValue().getNotifyDate()
                    ).containsExactlyInAnyOrder(
                            tuple(REMIND.name(), true, Frequency.WEEKLY.getKey(), dateConverter().convert(REMIND_DATE)),
                            tuple(BACKUP.name(), false, Frequency.MONTHLY.getKey(), dateConverter().convert(BACKUP_DATE))
                    );
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for PUT /recipients/current without authentication.
     */
    @Test
    public void shouldFailToUpdateCurrentRecipientSettingsWithoutAuthentication() {
        webClient.mutateWith(csrf())
                .put()
                .uri(URI)
                .bodyValue(stubRecipient())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private RecipientServiceProto.Recipient stubRecipientProto() {
        return RecipientServiceProto.Recipient.newBuilder()
                .setUserName(USERNAME)
                .setEmail(EMAIL)
                .putAllNotifications(stubNotificationSettingsProto())
                .build();
    }

    private Map<String, RecipientServiceProto.NotificationSettings> stubNotificationSettingsProto() {
        RecipientServiceProto.NotificationSettings backup = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(false)
                .setFrequency(Frequency.MONTHLY.getKey())
                .setNotifyDate(dateConverter().convert(BACKUP_DATE))
                .build();
        RecipientServiceProto.NotificationSettings remind = RecipientServiceProto.NotificationSettings.newBuilder()
                .setActive(true)
                .setFrequency(Frequency.WEEKLY.getKey())
                .setNotifyDate(dateConverter().convert(REMIND_DATE))
                .build();
        return ImmutableMap.of(
                BACKUP.name(), backup,
                REMIND.name(), remind
        );
    }

    private Recipient stubRecipient() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .notifyDate(BACKUP_DATE)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .notifyDate(REMIND_DATE)
                .build();
        return Recipient.builder()
                .email(EMAIL)
                .notification(BACKUP, backup)
                .notification(REMIND, remind)
                .build();
    }
}