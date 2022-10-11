package com.github.galleog.piggymetrics.account.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaAutoConfiguration;
import com.google.common.collect.ImmutableList;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import net.mguenther.kafka.junit.ExternalKafkaCluster;
import net.mguenther.kafka.junit.ReadKeyValues;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Integration tests for {@link AccountService}.
 */
@Testcontainers
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(classes = AccountServiceIntegrationTest.Config.class)
class AccountServiceIntegrationTest {
    private static final String ACCOUNT_SERVICE = "account-service";
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.2.2");
    private static final String USD = "USD";
    private static final String NAME = "test";
    private static final String NOTE = "note";

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(KAFKA_IMAGE);

    @Autowired
    private AccountRepository accountRepository;
    @GrpcClient(ACCOUNT_SERVICE)
    private ReactorAccountServiceGrpc.ReactorAccountServiceStub accountServiceStub;
    @Value("${spring.kafka.producer.topic}")
    private String topic;

    private ExternalKafkaCluster kafka;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        kafka = ExternalKafkaCluster.at(kafkaContainer.getBootstrapServers());
    }

    /**
     * Test should call the gRPC service and publish an {@link AccountUpdatedEvent}.
     */
    @Test
    void shouldCallServiceAndPublishEvent() throws Exception {
        var money = Money.of(BigDecimal.ZERO, USD);
        var saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(moneyConverter().convert(money))
                .setInterest(bigDecimalConverter().convert(BigDecimal.ZERO))
                .build();
        var account = AccountServiceProto.Account.newBuilder()
                .setName(NAME)
                .setSaving(saving)
                .addAllItems(ImmutableList.of())
                .setNote(NOTE)
                .build();

        when(accountRepository.update(any(Account.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        accountServiceStub.updateAccount(account)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        var consumedRecords = kafka.read(ReadKeyValues.from(topic, byte[].class)
                .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class));
        assertThat(consumedRecords).hasSize(1);
        assertThat(consumedRecords.get(0).getKey()).isEqualTo(NAME);
        assertThat(AccountUpdatedEvent.parseFrom(consumedRecords.get(0).getValue())).extracting(
                AccountUpdatedEvent::getAccountName,
                AccountUpdatedEvent::getItemsCount,
                AccountUpdatedEvent::getSaving,
                AccountUpdatedEvent::getNote
        ).containsExactly(NAME, 0, saving, NOTE);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            ReactiveKafkaAutoConfiguration.class,
            GrpcServerAutoConfiguration.class,
            GrpcServerFactoryAutoConfiguration.class,
            GrpcClientAutoConfiguration.class
    })
    static class Config {
        @Bean
        AccountRepository accountRepository() {
            return mock(AccountRepository.class);
        }

        @Bean
        AccountService accountService(@Value("${spring.kafka.producer.topic}") String topic, AccountRepository accountRepository,
                                      ReactiveKafkaProducerTemplate<String, AccountUpdatedEvent> producerTemplate) {
            return new AccountService(topic, accountRepository, producerTemplate);
        }
    }
}