package com.github.galleog.piggymetrics.keycloak.provider;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufSerializer;
import com.github.galleog.piggymetrics.auth.UserCreatedEventOuterClass.UserCreatedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Properties;

/**
 * <a href="https://www.keycloak.org/">Keycloak</a> provider factory for {@link PiggymetricsEventListenerProvider}.
 */
public class PiggymetricsEventListenerProviderFactory implements EventListenerProviderFactory {
    private static final String PROPERTIES_RESOURCE = "piggymetrics-kafka-producer.properties";
    private static final String ID = "piggymetrics";
    private static final String KAFKA_BROKERS = System.getenv("KAFKA_BROKERS");
    private static final String USER_EVENTS_TOPIC = System.getenv("USER_EVENTS_TOPIC");

    private Producer<String, UserCreatedEvent> producer;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new PiggymetricsEventListenerProvider(USER_EVENTS_TOPIC, producer);
    }

    @Override
    public void init(Config.Scope config) {
        Properties properties = new PropertiesReader().getProperties(PROPERTIES_RESOURCE);
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        producer = new KafkaProducer<>(properties, new StringSerializer(), new KafkaProtobufSerializer<>());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }

    @Override
    public String getId() {
        return ID;
    }
}
