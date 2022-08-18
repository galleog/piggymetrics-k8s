package com.github.galleog.piggymetrics.statistics.event;

import static com.github.galleog.piggymetrics.statistics.event.AccountUpdatedEventConsumer.BASE_CURRENCY;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.domain.TimePeriod;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.piggymetrics.statistics.service.MonetaryConversionService;
import com.github.galleog.protobuf.java.type.MoneyProto;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;

/**
 * Tests for {@link AccountUpdatedEventConsumer}.
 */
@ExtendWith(MockitoExtension.class)
class AccountUpdatedEventConsumerTest {
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

    @Mock
    private DataPointRepository dataPointRepository;
    @Mock
    private MonetaryConversionService conversionService;
    @Mock
    private TransactionalOperator operator;
    @Captor
    private ArgumentCaptor<DataPoint> dataPointCaptor;
    @InjectMocks
    private AccountUpdatedEventConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(operator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test for {@link AccountUpdatedEventConsumer#apply(Flux)} when creating a new data point.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveNewDataPoint() {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(any(DataPoint.class))).thenReturn(Mono.empty());
        when(dataPointRepository.save(dataPointCaptor.capture()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        consumer.apply(stubEvents())
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(dataPointCaptor.getValue().getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertThat(dataPointCaptor.getValue().getDate()).isEqualTo(LocalDate.now());
        assertThat(dataPointCaptor.getValue().getMetrics()).extracting(
                ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
        ).containsExactlyInAnyOrder(
                tuple(ItemType.EXPENSE, GROCERY, NORMALIZED_GROCERY_AMOUNT),
                tuple(ItemType.INCOME, SALARY, NORMALIZED_SALARY_AMOUNT)
        );
        assertThat(dataPointCaptor.getValue().getStatistics()).containsOnly(
                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, NORMALIZED_GROCERY_AMOUNT),
                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, NORMALIZED_SALARY_AMOUNT),
                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, NORMALIZED_SAVING_AMOUNT)
        );

        verify(operator).transactional(any(Mono.class));
    }

    /**
     * Test for {@link AccountUpdatedEventConsumer#apply(Flux)} when updating an existing data point.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateExistingDataPoint() {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(dataPointCaptor.capture()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        consumer.apply(stubEvents())
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(dataPointCaptor.getValue().getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertThat(dataPointCaptor.getValue().getDate()).isEqualTo(LocalDate.now());
        assertThat(dataPointCaptor.getValue().getMetrics()).extracting(
                ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
        ).containsExactlyInAnyOrder(
                tuple(ItemType.EXPENSE, GROCERY, NORMALIZED_GROCERY_AMOUNT),
                tuple(ItemType.INCOME, SALARY, NORMALIZED_SALARY_AMOUNT)
        );
        assertThat(dataPointCaptor.getValue().getStatistics()).containsOnly(
                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, NORMALIZED_GROCERY_AMOUNT),
                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, NORMALIZED_SALARY_AMOUNT),
                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, NORMALIZED_SAVING_AMOUNT)
        );

        verify(dataPointRepository, never()).save(any(DataPoint.class));
        verify(operator).transactional(any(Mono.class));
    }

    private Flux<AccountServiceProto.AccountUpdatedEvent> stubEvents() {
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
        var event = AccountServiceProto.AccountUpdatedEvent.newBuilder()
                .setAccountName(ACCOUNT_NAME)
                .addItems(grocery)
                .addItems(salary)
                .setSaving(saving)
                .build();
        return Flux.just(event);
    }
}