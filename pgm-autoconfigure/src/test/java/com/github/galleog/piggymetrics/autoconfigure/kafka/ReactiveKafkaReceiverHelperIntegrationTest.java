package com.github.galleog.piggymetrics.autoconfigure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration tests for {@link ReactiveKafkaReceiverHelper}.
 */
@Testcontainers
@ImportAutoConfiguration(ReactiveKafkaAutoConfiguration.class)
@SpringBootTest(classes = ReactiveKafkaReceiverHelperIntegrationTest.KafkaConfig.class)
class ReactiveKafkaReceiverHelperIntegrationTest {
    private static final String KEY = "key";
    private static final String VALUE = "test";
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.2.2");

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(KAFKA_IMAGE);

    @Autowired
    private ReactiveKafkaProducerTemplate<String, String> producerTemplate;
    @Autowired
    private AtomicInteger counter;
    @Value("${spring.kafka.consumer.subscribeTopics}")
    private String topic;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    /**
     * Test should send and receive one Kafka message.
     */
    @Test
    void shouldSendAndReceiveMessage() {
        producerTemplate.send(topic, KEY, VALUE).subscribe();

        await().untilAtomic(counter, is(1));
    }

    @Configuration(proxyBeanMethods = false)
    static class KafkaConfig {
        @Bean
        ReactiveKafkaReceiverHelper<String, String> receiverHelper(
                AtomicInteger counter,
                ReactiveKafkaConsumerTemplate<String, String> consumerTemplate
        ) {
            return new ReactiveKafkaReceiverHelper<>(consumerTemplate, records ->
                    records.doOnNext(record -> {
                        assertThat(record.key()).isEqualTo(KEY);
                        assertThat(record.value()).isEqualTo(VALUE);
                        counter.incrementAndGet();
                    }).then()
            );
        }

        @Bean
        AtomicInteger counter() {
            return new AtomicInteger(0);
        }
    }
}