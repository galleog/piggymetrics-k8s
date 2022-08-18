package com.github.galleog.piggymetrics.notification.event;

import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Consumer of events on new user registrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer implements Function<Flux<UserRegisteredEvent>, Mono<Void>> {
    private final RecipientRepository recipientRepository;
    private final TransactionalOperator operator;

    @Override
    public Mono<Void> apply(Flux<UserRegisteredEvent> events) {
        return events.doOnNext(event -> logger.info("UserRegisteredEvent for user '{}' received", event.getUserName()))
                .flatMap(this::doCreateRecipient)
                .then();
    }

    private Mono<Recipient> doCreateRecipient(UserRegisteredEvent event) {
        return recipientRepository.getByUsername(event.getUserName())
                .doOnNext(r -> logger.warn("Notification settings for user '{}' already exists", r.getUsername()))
                .hasElement()
                .filter(b -> !b)
                .map(b -> newRecipient(event))
                .flatMap(recipientRepository::save)
                .doOnNext(r -> logger.info("Notification settings for user '{}' created", r.getUsername()))
                .as(operator::transactional);
    }

    private Recipient newRecipient(UserRegisteredEvent event) {
        var remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.MONTHLY)
                .build();
        return Recipient.builder()
                .username(event.getUserName())
                .email(event.getEmail())
                .notification(NotificationType.REMIND, remind)
                .build();
    }
}
