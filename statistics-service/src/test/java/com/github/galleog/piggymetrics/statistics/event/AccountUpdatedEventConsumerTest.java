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
import com.github.galleog.piggymetrics.statistics.StatisticsApplication;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.domain.TimePeriod;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.piggymetrics.statistics.service.MonetaryConversionService;
import com.github.galleog.protobuf.java.type.MoneyProto;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;

/**
 * Tests for {@link AccountUpdatedEventConsumer}.
 */
@ActiveProfiles("test")
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ImportAutoConfiguration(exclude = {
        JooqAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        GrpcServerAutoConfiguration.class,
        GrpcServerFactoryAutoConfiguration.class
})
@SpringBootTest(classes = {
        StatisticsApplication.class,
        TestChannelBinderConfiguration.class
})
class AccountUpdatedEventConsumerTest {
    private static final String DESTINATION_NAME = "account-events";
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

    @Autowired
    private InputDestination input;
    @MockBean
    private DataPointRepository dataPointRepository;
    @MockBean
    private MonetaryConversionService conversionService;
    @Captor
    private ArgumentCaptor<DataPoint> dataPointCaptor;

    /**
     * Test for {@link AccountUpdatedEventConsumer#accept(AccountServiceProto.AccountUpdatedEvent)}
     * when creating a new data point.
     */
    @Test
    void shouldSaveNewDataPoint() {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(any(DataPoint.class))).thenReturn(Optional.empty());
        when(dataPointRepository.save(dataPointCaptor.capture()))
                .thenAnswer((Answer<DataPoint>) invocation -> invocation.getArgument(0));

        input.send(MessageBuilder.withPayload(stubEvent()).build(), DESTINATION_NAME);

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
    }

    /**
     * Test for {@link AccountUpdatedEventConsumer#accept(AccountServiceProto.AccountUpdatedEvent)}
     * when updating an existing data point.
     */
    @Test
    void shouldUpdateExistingDataPoint() {
        when(conversionService.convert(GROCERY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_GROCERY_AMOUNT);
        when(conversionService.convert(SALARY_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SALARY_AMOUNT);
        when(conversionService.convert(SAVING_AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING_AMOUNT);

        when(dataPointRepository.update(dataPointCaptor.capture()))
                .thenAnswer((Answer) invocation -> Optional.of(invocation.getArgument(0)));

        input.send(MessageBuilder.withPayload(stubEvent()).build(), DESTINATION_NAME);

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
    }

    private AccountServiceProto.AccountUpdatedEvent stubEvent() {
        AccountServiceProto.Item grocery = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.EXPENSE)
                .setTitle(GROCERY)
                .setMoney(GROCERY_PROTO_AMOUNT)
                .setPeriod(AccountServiceProto.TimePeriod.DAY)
                .build();
        AccountServiceProto.Item salary = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.INCOME)
                .setTitle(SALARY)
                .setMoney(SALARY_PROTO_AMOUNT)
                .setPeriod(AccountServiceProto.TimePeriod.YEAR)
                .build();
        AccountServiceProto.Saving saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(SAVING_PROTO_AMOUNT)
                .build();
        return AccountServiceProto.AccountUpdatedEvent.newBuilder()
                .setAccountName(ACCOUNT_NAME)
                .addItems(grocery)
                .addItems(salary)
                .setSaving(saving)
                .build();
    }
}