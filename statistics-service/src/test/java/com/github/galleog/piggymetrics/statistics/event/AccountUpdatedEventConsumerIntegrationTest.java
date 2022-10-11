package com.github.galleog.piggymetrics.statistics.event;

import static com.github.galleog.piggymetrics.statistics.event.AccountUpdatedEventConsumer.BASE_CURRENCY;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufSerializer;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.autoconfigure.kafka.ReactiveKafkaAutoConfiguration;
import com.github.galleog.piggymetrics.statistics.config.ReactiveKafkaConfig;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.domain.TimePeriod;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.piggymetrics.statistics.service.MonetaryConversionService;
import com.github.galleog.protobuf.java.type.MoneyProto;
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

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;

/**
 * Integration tests for {@link AccountUpdatedEventConsumer}.
 */
@Testcontainers
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@SpringBootTest(classes = AccountUpdatedEventConsumerIntegrationTest.Config.class)
class AccountUpdatedEventConsumerIntegrationTest {
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.2.2");
    private static final CurrencyUnit EUR = Monetary.getCurrency("EUR");
    private static final String ACCOUNT_NAME = "test";
    private static final String SALARY = "Salary";
    private static final Money SALARY_AMOUNT = Money.of(36000, EUR);
    private static final MoneyProto.Money SALARY_PROTO_AMOUNT = moneyConverter().convert(SALARY_AMOUNT);
    private static final Money CONVERTED_SALARY_AMOUNT = Money.of(40320, BASE_CURRENCY);
    private static final BigDecimal NORMALIZED_SALARY_AMOUNT = CONVERTED_SALARY_AMOUNT.divide(TimePeriod.YEAR.getBaseRatio())
            .getNumber().numberValue(BigDecimal.class);
    private static final String GROCERY = "Grocery";
    private static final Money GROCERY_AMOUNT = Money.of(10, EUR);
    private static final MoneyProto.Money GROCERY_PROTO_AMOUNT = moneyConverter().convert(GROCERY_AMOUNT);
    private static final Money CONVERTED_GROCERY_AMOUNT = Money.of(11.2, BASE_CURRENCY);
    private static final BigDecimal NORMALIZED_GROCERY_AMOUNT = CONVERTED_GROCERY_AMOUNT.getNumber().numberValue(BigDecimal.class);
    private static final Money SAVING_AMOUNT = Money.of(5900, EUR);
    private static final MoneyProto.Money SAVING_PROTO_AMOUNT = moneyConverter().convert(SAVING_AMOUNT);
    private static final Money CONVERTED_SAVING_AMOUNT = Money.of(6608, BASE_CURRENCY);
    private static final BigDecimal NORMALIZED_SAVING_AMOUNT = CONVERTED_SAVING_AMOUNT.getNumber().numberValue(BigDecimal.class);
    private static final long TIMEOUT = 10000L;

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(KAFKA_IMAGE);

    @Autowired
    private DataPointRepository dataPointRepository;
    @Autowired
    private MonetaryConversionService conversionService;
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
     * Test for {@link AccountUpdatedEventConsumer#apply(Flux)} when creating a new data point.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveNewDataPoint() throws InterruptedException {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(any(DataPoint.class))).thenReturn(Mono.empty());
        when(dataPointRepository.save(any(DataPoint.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sendEvent();

        verify(dataPointRepository, timeout(TIMEOUT)).save(argThat(dataPoint -> {
            assertThat(dataPoint.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(dataPoint.getDate()).isEqualTo(LocalDate.now());
            assertThat(dataPoint.getMetrics()).extracting(
                    ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(ItemType.EXPENSE, GROCERY, NORMALIZED_GROCERY_AMOUNT),
                    tuple(ItemType.INCOME, SALARY, NORMALIZED_SALARY_AMOUNT)
            );
            assertThat(dataPoint.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, NORMALIZED_GROCERY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, NORMALIZED_SALARY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, NORMALIZED_SAVING_AMOUNT)
            );
            return true;
        }));
        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
    }

    /**
     * Test for {@link AccountUpdatedEventConsumer#apply(Flux)} when updating an existing data point.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateExistingDataPoint() throws InterruptedException {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(any(DataPoint.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sendEvent();

        verify(dataPointRepository, timeout(TIMEOUT)).update(argThat(dataPoint -> {
            assertThat(dataPoint.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(dataPoint.getDate()).isEqualTo(LocalDate.now());
            assertThat(dataPoint.getMetrics()).extracting(
                    ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(ItemType.EXPENSE, GROCERY, NORMALIZED_GROCERY_AMOUNT),
                    tuple(ItemType.INCOME, SALARY, NORMALIZED_SALARY_AMOUNT)
            );
            assertThat(dataPoint.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, NORMALIZED_GROCERY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, NORMALIZED_SALARY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, NORMALIZED_SAVING_AMOUNT)
            );
            return true;
        }));
        verify(operator, timeout(TIMEOUT)).transactional(any(Mono.class));
        verify(dataPointRepository, timeout(TIMEOUT).times(0)).save(any(DataPoint.class));
    }

    private KeyValue<String, AccountUpdatedEvent> stubEvent() {
        var grocery = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.EXPENSE)
                .setTitle(GROCERY)
                .setMoney(GROCERY_PROTO_AMOUNT)
                .setPeriod(AccountServiceProto.TimePeriod.DAY)
                .build();
        var salary = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.INCOME)
                .setTitle(SALARY)
                .setMoney(SALARY_PROTO_AMOUNT)
                .setPeriod(AccountServiceProto.TimePeriod.YEAR)
                .build();
        var saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(SAVING_PROTO_AMOUNT)
                .build();
        var event = AccountUpdatedEvent.newBuilder()
                .setAccountName(ACCOUNT_NAME)
                .addItems(grocery)
                .addItems(salary)
                .setSaving(saving)
                .build();
        return new KeyValue<>(ACCOUNT_NAME, event);
    }

    private void sendEvent() throws InterruptedException {
        kafka.send(SendKeyValues.to(topic, ImmutableList.of(stubEvent()))
                .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ReactiveKafkaConfig.class)
    @ImportAutoConfiguration(ReactiveKafkaAutoConfiguration.class)
    static class Config {
        @Bean
        MonetaryConversionService conversionService() {
            return mock(MonetaryConversionService.class);
        }

        @Bean
        DataPointRepository dataPointRepository() {
            return mock(DataPointRepository.class);
        }

        @Bean
        TransactionalOperator operator() {
            return mock(TransactionalOperator.class);
        }

        @Bean
        AccountUpdatedEventConsumer consumer(
                MonetaryConversionService conversionService,
                DataPointRepository dataPointRepository,
                TransactionalOperator operator
        ) {
            return new AccountUpdatedEventConsumer(conversionService, dataPointRepository, operator);
        }
    }
}