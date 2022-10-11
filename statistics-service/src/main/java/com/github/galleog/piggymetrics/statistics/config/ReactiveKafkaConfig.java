package com.github.galleog.piggymetrics.statistics.config;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufDeserializer;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaReceiverHelper;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReceiverOptionsCustomizer;
import com.github.galleog.piggymetrics.statistics.event.AccountUpdatedEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

/**
 * Configuration for reactive Kafka.
 */
@Configuration(proxyBeanMethods = false)
public class ReactiveKafkaConfig {
    @Bean
    ReceiverOptionsCustomizer<String, AccountUpdatedEvent> receiverOptionsCustomizer() {
        return options -> options.withValueDeserializer(new KafkaProtobufDeserializer<>(AccountUpdatedEvent.parser()));
    }

    @Bean
    ReactiveKafkaReceiverHelper<String, AccountUpdatedEvent> receiverHelper(
            ReactiveKafkaConsumerTemplate<String, AccountUpdatedEvent> consumerTemplate,
            AccountUpdatedEventConsumer consumer
    ) {
        return new ReactiveKafkaReceiverHelper<>(consumerTemplate, consumer);
    }
}
