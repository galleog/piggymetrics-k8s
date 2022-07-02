package com.github.galleog.piggymetrics.keycloak.provider;

import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

/**
 * Event listener that sends a {@link UserRegisteredEvent} when a new user is registered.
 */
@RequiredArgsConstructor
public class PiggymetricsEventListenerProvider implements EventListenerProvider {
    @VisibleForTesting
    static final String USERNAME_KEY = "username";
    @VisibleForTesting
    static final String EMAIL_KEY = "email";

    private static final Logger logger = Logger.getLogger(PiggymetricsEventListenerProvider.class);

    @NonNull
    private final String topic;
    @NonNull
    private final Producer<String, UserRegisteredEvent> producer;

    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())) {
            UserRegisteredEvent ure = UserRegisteredEvent.newBuilder()
                    .setUserId(event.getUserId())
                    .setUserName(event.getDetails().get(USERNAME_KEY))
                    .setEmail(event.getDetails().get(EMAIL_KEY))
                    .build();
            ProducerRecord<String, UserRegisteredEvent> record = new ProducerRecord<>(topic, event.getUserId(), ure);
            producer.send(record, ((metadata, exception) -> {
                if (metadata != null) {
                    logger.info("Message on registration of user '" + record.key() + "' sent");
                } else {
                    logger.error("Failed to send message on registration of user '" + record.key() + "'", exception);
                }
            }));
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }
}
