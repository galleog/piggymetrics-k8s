package com.github.galleog.piggymetrics.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufSerializer;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaAutoConfiguration;
import com.github.galleog.piggymetrics.notification.config.ReactiveKafkaConfig;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import net.mguenther.kafka.junit.ExternalKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValues;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Integration tests for {@link UserRegisteredEventConsumer}.
 */
@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@SpringBootTest(classes = UserRegisteredEventConsumerIntegrationTest.Config.class)
class UserRegisteredEventConsumerIntegrationTest {
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.2.2");
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final KeyValue<String, UserRegisteredEvent> EVENT = new KeyValue<>(
            USERNAME,
            UserRegisteredEvent.newBuilder()
                    .setUserId(UUID.randomUUID().toString())
                    .setUserName(USERNAME)
                    .setEmail(EMAIL)
                    .build()
    );
    private static final long TIMEOUT = 10000L;

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(KAFKA_IMAGE);

    @Autowired
    private RecipientRepository recipientRepository;
    @Autowired
    private TransactionalOperator operator;
    @Value("${spring.kafka.consumer.subscribeTopics}")
    private String topic;

    private ExternalKafkaCluster kafka;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafka = ExternalKafkaCluster.at(kafkaContainer.getBootstrapServers());

        when(operator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateRemindNotification() throws InterruptedException {
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Mono.empty());
        when(recipientRepository.save(any(Recipient.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sendEvent();

        verify(recipientRepository, timeout(TIMEOUT)).save(argThat(recipient -> {
            assertThat(recipient.getUsername()).isEqualTo(USERNAME);
            assertThat(recipient.getEmail()).isEqualTo(EMAIL);
            assertThat(recipient.getNotifications()).containsOnlyKeys(NotificationType.REMIND);
            assertThat(recipient.getNotifications().get(NotificationType.REMIND)).extracting(
                    NotificationSettings::isActive, NotificationSettings::getFrequency
            ).containsExactly(true, Frequency.MONTHLY);
            return true;
        }));
        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)} when notification settings for the same recipient already exist.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldNotCreateNotificationsWhenAlreadyExist() throws InterruptedException {
        var recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .build();
        when(recipientRepository.getByUsername(USERNAME)).thenReturn(Mono.just(recipient));

        sendEvent();

        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
        verify(recipientRepository, timeout(TIMEOUT).times(0)).save(any(Recipient.class));
    }

    private void sendEvent() throws InterruptedException {
        kafka.send(SendKeyValues.to(topic, ImmutableList.of(EVENT))
                .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ReactiveKafkaConfig.class)
    @ImportAutoConfiguration(ReactiveKafkaAutoConfiguration.class)
    static class Config {
        @Bean
        RecipientRepository recipientRepository() {
            return mock(RecipientRepository.class);
        }

        @Bean
        TransactionalOperator operator() {
            return mock(TransactionalOperator.class);
        }

        @Bean
        UserRegisteredEventConsumer consumer(RecipientRepository recipientRepository, TransactionalOperator operator) {
            return new UserRegisteredEventConsumer(recipientRepository, operator);
        }
    }
}