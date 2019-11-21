package com.github.galleog.piggymetrics.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

/**
 * Tests for {@link UserRegisteredEventConsumer}.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({SpringExtension.class, MockitoExtension.class})
class UserRegisteredEventConsumerTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final UserRegisteredEvent EVENT = UserRegisteredEvent.newBuilder()
            .setUserId(UUID.randomUUID().toString())
            .setUserName(USERNAME)
            .setEmail(EMAIL)
            .build();

    @Autowired
    private Sink sink;
    @MockBean
    private RecipientRepository recipientRepository;
    @Captor
    private ArgumentCaptor<Recipient> recipientCaptor;

    /**
     * Test for {@link UserRegisteredEventConsumer#createRemindNotification(UserRegisteredEvent)}.
     */
    @Test
    void shouldCreateRemindNotification() {
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Optional.empty());
        doNothing().when(recipientRepository).save(recipientCaptor.capture());

        sink.input().send(MessageBuilder.withPayload(EVENT).build());

        assertThat(recipientCaptor.getValue().getUsername()).isEqualTo(USERNAME);
        assertThat(recipientCaptor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(recipientCaptor.getValue().getNotifications()).containsOnlyKeys(NotificationType.REMIND);
        assertThat(recipientCaptor.getValue().getNotifications().get(NotificationType.REMIND)).extracting(
                NotificationSettings::isActive, NotificationSettings::getFrequency
        ).containsExactly(true, Frequency.MONTHLY);
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#createRemindNotification(UserRegisteredEvent)}
     * when notification settings for the same recipient already exist.
     */
    @Test
    void shouldNotCreateNotificationsWhenAlreadyExist() {
        Recipient recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .build();
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Optional.of(recipient));

        sink.input().send(MessageBuilder.withPayload(EVENT).build());

        verify(recipientRepository, never()).save(any(Recipient.class));
    }
}