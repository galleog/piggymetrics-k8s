package com.github.galleog.piggymetrics.statistics.service;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.BASE_CURRENCY;
import static com.github.galleog.piggymetrics.statistics.domain.StatisticMetric.EXPENSES_AMOUNT;
import static com.github.galleog.piggymetrics.statistics.domain.StatisticMetric.INCOMES_AMOUNT;
import static com.github.galleog.piggymetrics.statistics.domain.StatisticMetric.SAVING_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.DataPointId;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link StatisticsService}.
 */
@ExtendWith(SpringExtension.class)
class StatisticsServiceTest {
    private static final String ACCOUNT_NAME = "test";
    private static final CurrencyUnit RUB = Monetary.getCurrency("RUB");
    private static final ItemMetric GROCERY = ItemMetric.builder()
            .type(ItemType.EXPENSE)
            .title("Grocery")
            .moneyAmount(Money.of(10, BASE_CURRENCY))
            .build();
    private static final ItemMetric VACATION = ItemMetric.builder()
            .type(ItemType.EXPENSE)
            .title("Vacation")
            .moneyAmount(Money.of(140, BASE_CURRENCY))
            .build();
    private static final ItemMetric SALARY = ItemMetric.builder()
            .type(ItemType.INCOME)
            .title("Salary")
            .moneyAmount(Money.of(300, BASE_CURRENCY))
            .build();
    private static final Money SAVING = Money.of(100000, RUB);
    private static final Money CONVERTED_SAVING = Money.of(1605, BASE_CURRENCY);

    @MockBean
    private DataPointRepository repository;
    @MockBean
    private ConversionService conversionService;
    @Autowired
    private StatisticsService statisticsService;

    /**
     * Test for {@link StatisticsService#findByAccountName(String)}.
     */
    @Test
    void shouldFindDataPointsByAccountName() {
        when(conversionService.convert(any(Money.class), eq(BASE_CURRENCY)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DataPoint dataPoint = DataPoint.builder()
                .account(ACCOUNT_NAME)
                .saving(Money.of(0, BASE_CURRENCY))
                .build();
        when(repository.findByIdAccount("test")).thenReturn(ImmutableList.of(dataPoint));

        List<DataPoint> result = statisticsService.findByAccountName(ACCOUNT_NAME);
        assertThat(result).containsExactly(dataPoint);
    }

    /**
     * Test for {@link StatisticsService#findByAccountName(String)} when the account name is empty.
     */
    @Test
    void shouldFailToFindDataPointsWhenAccountNameIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> statisticsService.findByAccountName(StringUtils.EMPTY));
    }

    /**
     * Test for {@link StatisticsService#save(String, Collection, Money)} when creating a new data point.
     */
    @Test
    void shouldSaveNewDataPoint() {
        when(conversionService.convert(SAVING, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING);
        when(repository.findById(any(DataPointId.class))).thenReturn(Optional.empty());

        DataPoint dataPoint = statisticsService.save(ACCOUNT_NAME, ImmutableList.of(GROCERY, SALARY), SAVING);
        assertThat(dataPoint.getAccount()).isEqualTo(ACCOUNT_NAME);
        assertThat(dataPoint.getDate()).isEqualTo(LocalDate.now());
        assertThat(dataPoint.getExpenses()).containsExactly(GROCERY);
        assertThat(dataPoint.getIncomes()).containsExactly(SALARY);
        assertThat(dataPoint.getStatistics()).containsOnly(
                new SimpleEntry<>(EXPENSES_AMOUNT, GROCERY.getMoneyAmount()),
                new SimpleEntry<>(INCOMES_AMOUNT, SALARY.getMoneyAmount()),
                new SimpleEntry<>(SAVING_AMOUNT, CONVERTED_SAVING)
        );
    }

    /**
     * Test for {@link StatisticsService#save(String, Collection, Money)} when updating an existing data point.
     */
    @Test
    void shouldUpdateExistingDataPoint() {
        Money saving = Money.of(0, BASE_CURRENCY);
        when(conversionService.convert(SAVING, BASE_CURRENCY)).thenReturn(CONVERTED_SAVING);
        when(conversionService.convert(saving, BASE_CURRENCY)).thenReturn(saving);

        LocalDate now = LocalDate.now();
        DataPoint dataPoint = DataPoint.builder()
                .account(ACCOUNT_NAME)
                .date(now)
                .metric(VACATION)
                .saving(saving)
                .build();
        assertThat(dataPoint.getStatistics().get(SAVING_AMOUNT)).isEqualTo(saving);

        when(repository.findById(DataPointId.of(ACCOUNT_NAME, now))).thenReturn(Optional.of(dataPoint));

        statisticsService.save(ACCOUNT_NAME, ImmutableList.of(GROCERY, SALARY), SAVING);
        assertThat(dataPoint.getAccount()).isEqualTo(ACCOUNT_NAME);
        assertThat(dataPoint.getDate()).isEqualTo(now);
        assertThat(dataPoint.getExpenses()).containsExactly(GROCERY);
        assertThat(dataPoint.getIncomes()).containsExactly(SALARY);
        assertThat(dataPoint.getStatistics()).containsOnly(
                new SimpleEntry<>(EXPENSES_AMOUNT, GROCERY.getMoneyAmount()),
                new SimpleEntry<>(INCOMES_AMOUNT, SALARY.getMoneyAmount()),
                new SimpleEntry<>(SAVING_AMOUNT, CONVERTED_SAVING)
        );
    }

    @Configuration
    @EnableSpringConfigured
    static class Config {
        @Bean
        public StatisticsService statisticsService(DataPointRepository repository) {
            return new StatisticsService(repository);
        }
    }
}