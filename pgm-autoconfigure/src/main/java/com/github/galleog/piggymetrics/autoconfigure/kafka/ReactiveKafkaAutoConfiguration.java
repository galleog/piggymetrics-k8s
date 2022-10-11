package com.github.galleog.piggymetrics.autoconfigure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collection;

/**
 * Auto-configuration for <a href="https://projectreactor.io/docs/kafka/release/reference/">Reactor Kafka</a>.
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaProperties.class)
public class ReactiveKafkaAutoConfiguration {
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaSender.class)
    static class ReactiveKafkaProducerTemplateConfiguration {
        @Bean
        @ConditionalOnMissingBean({SenderOptions.class, ReactiveKafkaProducerTemplate.class})
        <K, V> SenderOptions<K, V> senderOptions(
                KafkaProperties kafkaProperties,
                ObjectProvider<SenderOptionsCustomizer<K, V>> customizer
        ) {
            var options = SenderOptions.<K, V>create(kafkaProperties.buildProducerProperties());
            var dependency = customizer.getIfAvailable();
            return dependency != null ? dependency.customize(options) : options;
        }

        @Bean
        @ConditionalOnSingleCandidate(SenderOptions.class)
        @ConditionalOnMissingBean(ReactiveKafkaProducerTemplate.class)
        <K, V> ReactiveKafkaProducerTemplate<K, V> producerTemplate(SenderOptions<K, V> options) {
            return new ReactiveKafkaProducerTemplate<>(options);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaReceiver.class)
    static class ReactiveKafkaConsumerTemplateConfiguration {
        @Bean
        @ConditionalOnMissingBean({ReceiverOptions.class, ReactiveKafkaConsumerTemplate.class})
        @ConditionalOnProperty("spring.kafka.consumer.subscribeTopics")
        <K, V> ReceiverOptions<K, V> receiverOptions(
                KafkaProperties kafkaProperties,
                @Value("${spring.kafka.consumer.subscribeTopics}") Collection<String> topics,
                ObjectProvider<ReceiverOptionsCustomizer<K, V>> customizer
        ) {
            var options = ReceiverOptions.<K, V>create(kafkaProperties.buildConsumerProperties())
                    .subscription(topics);
            var dependency = customizer.getIfAvailable();
            return dependency != null ? dependency.customize(options) : options;
        }

        @Bean
        @ConditionalOnSingleCandidate(ReceiverOptions.class)
        @ConditionalOnMissingBean(ReactiveKafkaConsumerTemplate.class)
        <K, V> ReactiveKafkaConsumerTemplate<K, V> consumerTemplate(ReceiverOptions<K, V> options) {
            return new ReactiveKafkaConsumerTemplate<>(options);
        }
    }
}
