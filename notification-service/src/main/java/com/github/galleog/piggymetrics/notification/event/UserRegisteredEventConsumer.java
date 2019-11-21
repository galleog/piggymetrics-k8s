package com.github.galleog.piggymetrics.notification.event;

import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer of events on user creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {
    private final RecipientRepository recipientRepository;

    @Transactional
    @StreamListener(Sink.INPUT)
    public void createRemindNotification(UserRegisteredEvent event) {
        logger.info("UserCreated event for user '{}' received", event.getUserName());

        if (recipientRepository.getByUsername(event.getUserName()).isPresent()) {
            logger.warn("Notification settings for user '{}' already exists", event.getUserName());
            return;
        }

        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.MONTHLY)
                .build();
        Recipient recipient = Recipient.builder()
                .username(event.getUserName())
                .email(event.getEmail())
                .notification(NotificationType.REMIND, remind)
                .build();
        recipientRepository.save(recipient);

        logger.info("Notification settings for user '{}' created", event.getUserName());
    }
}
