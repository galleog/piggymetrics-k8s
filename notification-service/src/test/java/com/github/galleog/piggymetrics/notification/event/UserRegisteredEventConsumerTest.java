package com.github.galleog.piggymetrics.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

/**
 * Tests for {@link UserRegisteredEventConsumer}.
 */
@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final Flux<UserRegisteredEvent> EVENTS = Flux.just(
            UserRegisteredEvent.newBuilder()
                    .setUserId(UUID.randomUUID().toString())
                    .setUserName(USERNAME)
                    .setEmail(EMAIL)
                    .build()
    );

    @Mock
    private RecipientRepository recipientRepository;
    @Mock
    private TransactionalOperator operator;
    @Captor
    private ArgumentCaptor<Recipient> recipientCaptor;
    @InjectMocks
    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(operator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateRemindNotification() {
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Mono.empty());
        when(recipientRepository.save(recipientCaptor.capture()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        consumer.apply(EVENTS)
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(recipientCaptor.getValue().getUsername()).isEqualTo(USERNAME);
        assertThat(recipientCaptor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(recipientCaptor.getValue().getNotifications()).containsOnlyKeys(NotificationType.REMIND);
        assertThat(recipientCaptor.getValue().getNotifications().get(NotificationType.REMIND)).extracting(
                NotificationSettings::isActive, NotificationSettings::getFrequency
        ).containsExactly(true, Frequency.MONTHLY);

        verify(operator).transactional(any(Mono.class));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)} when notification settings for the same recipient already exist.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldNotCreateNotificationsWhenAlreadyExist() {
        var recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .build();
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Mono.just(recipient));

        consumer.apply(EVENTS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(recipientRepository, never()).save(any(Recipient.class));
        verify(operator).transactional(any(Mono.class));
    }
}