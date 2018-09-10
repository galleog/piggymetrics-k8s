package com.github.galleog.piggymetrics.statistics.acl;

import static com.github.galleog.piggymetrics.statistics.acl.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.BASE_CURRENCY;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;

/**
 * Tests for {@link ItemConverter}.
 */
@ExtendWith(MockitoExtension.class)
class ItemConverterTest {
    private static final String TITLE = "title";
    private static final Money AMOUNT = Money.of(345, "EUR");
    private static final Money CONVERTED_AMOUNT = Money.of(400, BASE_CURRENCY);

    @Mock
    private ConversionService conversionService;

    @BeforeEach
    void setUp() {
        when(conversionService.convert(AMOUNT, BASE_CURRENCY)).thenReturn(CONVERTED_AMOUNT);
    }

    /**
     * Test for conversion of an expense.
     */
    @ParameterizedTest
    @EnumSource(TimePeriod.class)
    void shouldConvertExpense(TimePeriod period) {
        Item expense = Item.builder()
                .title(TITLE)
                .moneyAmount(AMOUNT)
                .period(period)
                .build();
        ItemMetric metric = new ItemConverter(conversionService, EXPENSE).convert(expense);
        assertThat(metric.getType()).isEqualTo(EXPENSE);
        assertThat(metric.getTitle()).isEqualTo(TITLE);
        assertThat(metric.getMoneyAmount()).isEqualTo(CONVERTED_AMOUNT.divide(period.getBaseRatio()));
    }

    /**
     * Test for conversion of an income.
     */
    @Test
    void shouldConvertIncome() {
        Item income = Item.builder()
                .title(TITLE)
                .moneyAmount(AMOUNT)
                .period(DAY)
                .build();
        ItemMetric metric = new ItemConverter(conversionService, INCOME).convert(income);
        assertThat(metric.getType()).isEqualTo(INCOME);
        assertThat(metric.getTitle()).isEqualTo(TITLE);
        assertThat(metric.getMoneyAmount()).isEqualTo(CONVERTED_AMOUNT);
    }

    /**
     * Test for reverse conversion.
     */
    @Test
    void shouldFailToConvertReversely() {
        ItemMetric metric = ItemMetric.builder()
                .type(EXPENSE)
                .title(TITLE)
                .moneyAmount(CONVERTED_AMOUNT)
                .build();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> new ItemConverter(conversionService, EXPENSE).reverse().convert(metric));
    }
}