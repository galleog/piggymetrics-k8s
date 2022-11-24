package com.github.galleog.piggymetrics.account.event;

import static com.github.galleog.piggymetrics.account.event.UserRegisteredEventConsumer.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufSerializer;
import com.github.galleog.piggymetrics.account.config.ReactiveKafkaConfig;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaAutoConfiguration;
import net.mguenther.kafka.junit.ExternalKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValues;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.javamoney.moneta.Money;
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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Integration tests for {@link UserRegisteredEventConsumer}.
 */
@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@SpringBootTest(classes = UserRegisteredEventConsumerIntegrationTest.Config.class)
class UserRegisteredEventConsumerIntegrationTest {
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.2.2");
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String USERNAME = "test";
    private static final KeyValue<String, UserRegisteredEvent> EVENT = new KeyValue<>(
            USER_ID,
            UserRegisteredEvent.newBuilder()
                    .setUserId(USER_ID)
                    .setUserName(USERNAME)
                    .setEmail("test@example.com")
                    .build()
    );
    private static final long TIMEOUT = 10000L;

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(KAFKA_IMAGE);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionalOperator operator;
    @Value("${spring.kafka.consumer.subscribeTopics}")
    private String topic;

    private ExternalKafkaCluster kafka;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
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
    void shouldCreateAccount() throws InterruptedException {
        when(accountRepository.getByName(USERNAME)).thenReturn(Mono.empty());
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sendEvent();

        verify(accountRepository, timeout(TIMEOUT)).save(argThat(account -> {
            assertThat(account.getName()).isEqualTo(USERNAME);
            assertThat(account.getSaving()).extracting(
                    Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
            ).containsExactly(Money.of(BigDecimal.ZERO, BASE_CURRENCY), BigDecimal.ZERO, false, false);
            assertThat(account.getItems()).isEmpty();
            return true;
        }));
        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)} when an account with the same name already exists.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldNotCreateAccountWhenAlreadyExists() throws InterruptedException {
        var saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.TEN, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .build();
        var account = Account.builder()
                .name(USERNAME)
                .saving(saving)
                .build();
        when(accountRepository.getByName(USERNAME)).thenReturn(Mono.just(account));

        sendEvent();

        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
        verify(accountRepository, timeout(TIMEOUT).times(0)).save(any(Account.class));
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
        AccountRepository accountRepository() {
            return mock(AccountRepository.class);
        }

        @Bean
        TransactionalOperator operator() {
            return mock(TransactionalOperator.class);
        }

        @Bean
        UserRegisteredEventConsumer consumer(AccountRepository accountRepository, TransactionalOperator operator) {
            return new UserRegisteredEventConsumer(accountRepository, operator);
        }
    }
}