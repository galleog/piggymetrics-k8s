package com.github.galleog.piggymetrics.keycloak.provider;

import static com.github.galleog.piggymetrics.keycloak.provider.PiggymetricsEventListenerProvider.EMAIL_KEY;
import static com.github.galleog.piggymetrics.keycloak.provider.PiggymetricsEventListenerProvider.USERNAME_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.auth.UserCreatedEventOuterClass.UserCreatedEvent;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link PiggymetricsEventListenerProvider}.
 */
@ExtendWith(MockitoExtension.class)
class PiggymetricsEventListenerProviderTest {
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String TOPIC = "test-topic";

    @Mock
    private Producer<String, UserCreatedEvent> producer;
    @Captor
    private ArgumentCaptor<ProducerRecord<String, UserCreatedEvent>> captor;
    private PiggymetricsEventListenerProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PiggymetricsEventListenerProvider(TOPIC, producer);
    }

    /**
     * Test for {@link PiggymetricsEventListenerProvider#onEvent(Event)}.
     */
    @Test
    void shouldSendEvent() {
        when(producer.send(captor.capture(), any(Callback.class))).thenReturn(CompletableFuture.completedFuture(null));

        Event event = new Event();
        event.setType(EventType.REGISTER);
        event.setUserId(USER_ID);
        event.setDetails(ImmutableMap.of(USERNAME_KEY, USERNAME, EMAIL_KEY, EMAIL));
        provider.onEvent(event);

        ProducerRecord<String, UserCreatedEvent> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo(USER_ID);
        assertThat(record.value().getUserId()).isEqualTo(USER_ID);
        assertThat(record.value().getUserName()).isEqualTo(USERNAME);
        assertThat(record.value().getEmail()).isEqualTo(EMAIL);
    }
}