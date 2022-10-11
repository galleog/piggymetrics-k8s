package com.github.galleog.piggymetrics.notification.config;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufDeserializer;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaReceiverHelper;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReceiverOptionsCustomizer;
import com.github.galleog.piggymetrics.notification.event.UserRegisteredEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

/**
 * Configuration for reactive Kafka.
 */
@Configuration(proxyBeanMethods = false)
public class ReactiveKafkaConfig {
    @Bean
    ReceiverOptionsCustomizer<String, UserRegisteredEvent> receiverOptionsCustomizer() {
        return options -> options.withValueDeserializer(new KafkaProtobufDeserializer<>(UserRegisteredEvent.parser()));
    }

    @Bean
    ReactiveKafkaReceiverHelper<String, UserRegisteredEvent> receiverHelper(
            ReactiveKafkaConsumerTemplate<String, UserRegisteredEvent> consumerTemplate,
            UserRegisteredEventConsumer consumer
    ) {
        return new ReactiveKafkaReceiverHelper<>(consumerTemplate, consumer);
    }
}
