package com.github.galleog.piggymetrics.statistics.service;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.updateStatistics;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.protobuf.java.type.BigDecimalProto;
import com.google.common.collect.ImmutableList;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;

/**
 * Tests for {@link StatisticsService}.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    private static final String ACCOUNT_NAME = "test";
    private static final LocalDate DATE_1 = LocalDate.now().minusDays(5);
    private static final LocalDate DATE_2 = LocalDate.now().minusDays(2);
    private static final String SALARY = "Salary";
    private static final BigDecimal SALARY_AMOUNT = BigDecimal.valueOf(30000, 2);
    private static final BigDecimalProto.BigDecimal SALARY_PROTO_AMOUNT = bigDecimalConverter().convert(SALARY_AMOUNT);
    private static final String GROCERY = "Grocery";
    private static final BigDecimal GROCERY_AMOUNT = BigDecimal.valueOf(1000, 2);
    private static final BigDecimalProto.BigDecimal GROCERY_PROTO_AMOUNT = bigDecimalConverter().convert(GROCERY_AMOUNT);
    private static final String VACATION = "Vacation";
    private static final BigDecimal VACATION_AMOUNT = BigDecimal.valueOf(11300, 2);
    private static final BigDecimalProto.BigDecimal VACATION_PROTO_AMOUNT = bigDecimalConverter().convert(VACATION_AMOUNT);
    private static final BigDecimal EXPENSES_AMOUNT = GROCERY_AMOUNT.add(VACATION_AMOUNT);
    private static final BigDecimalProto.BigDecimal EXPENSES_PROTO_AMOUNT = bigDecimalConverter().convert(EXPENSES_AMOUNT);
    private static final BigDecimal SAVING_AMOUNT = BigDecimal.valueOf(590000, 2);
    private static final BigDecimalProto.BigDecimal SAVING_PROTO_AMOUNT = bigDecimalConverter().convert(SAVING_AMOUNT);

    @Mock
    private DataPointRepository dataPointRepository;
    @InjectMocks
    private StatisticsService statisticsService;

    /**
     * Test for {@link StatisticsService#listDataPoints(Mono)}.
     */
    @Test
    void shouldListDataPoints() {
        when(dataPointRepository.listByAccountName(ACCOUNT_NAME)).thenReturn(
                Flux.just(
                        stubDataPoint(DATE_1, SAVING_AMOUNT, salary()),
                        stubDataPoint(DATE_2, SAVING_AMOUNT, grocery(), vacation())
                )
        );

        statisticsService.listDataPoints(stubListDataPointsRequest())
                .as(StepVerifier::create)
                .expectNextMatches(dp -> {
                    assertThat(dp.getAccountName()).isEqualTo(ACCOUNT_NAME);
                    assertThat(dp.getDate()).isEqualTo(dateConverter().convert(DATE_1));
                    assertThat(dp.getMetricsList()).extracting(
                            StatisticsServiceProto.ItemMetric::getType,
                            StatisticsServiceProto.ItemMetric::getTitle,
                            StatisticsServiceProto.ItemMetric::getMoneyAmount
                    ).containsExactlyInAnyOrder(
                            tuple(StatisticsServiceProto.ItemType.INCOME, SALARY, SALARY_PROTO_AMOUNT)
                    );
                    assertThat(dp.getStatisticsMap()).containsOnly(
                            new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT.name(), SALARY_PROTO_AMOUNT),
                            new SimpleEntry<>(
                                    StatisticalMetric.EXPENSES_AMOUNT.name(), bigDecimalConverter().convert(BigDecimal.ZERO)
                            ),
                            new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT.name(), SAVING_PROTO_AMOUNT)
                    );
                    return true;
                }).expectNextMatches(dp -> {
                    assertThat(dp.getAccountName()).isEqualTo(ACCOUNT_NAME);
                    assertThat(dp.getDate()).isEqualTo(dateConverter().convert(DATE_2));
                    assertThat(dp.getMetricsList()).extracting(
                            StatisticsServiceProto.ItemMetric::getType,
                            StatisticsServiceProto.ItemMetric::getTitle,
                            StatisticsServiceProto.ItemMetric::getMoneyAmount
                    ).containsExactlyInAnyOrder(
                            tuple(StatisticsServiceProto.ItemType.EXPENSE, GROCERY, GROCERY_PROTO_AMOUNT),
                            tuple(StatisticsServiceProto.ItemType.EXPENSE, VACATION, VACATION_PROTO_AMOUNT)
                    );
                    assertThat(dp.getStatisticsMap()).containsOnly(
                            new SimpleEntry<>(
                                    StatisticalMetric.INCOMES_AMOUNT.name(), bigDecimalConverter().convert(BigDecimal.ZERO)
                            ),
                            new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT.name(), EXPENSES_PROTO_AMOUNT),
                            new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT.name(), SAVING_PROTO_AMOUNT)
                    );
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link StatisticsService#listDataPoints(Mono)} when no data points are found.
     */
    @Test
    void shouldFailToListDataPoints() {
        when(dataPointRepository.listByAccountName(ACCOUNT_NAME)).thenReturn(Flux.empty());

        statisticsService.listDataPoints(stubListDataPointsRequest())
                .as(StepVerifier::create)
                .expectErrorMatches(e -> {
                    assertThat(Status.fromThrowable(e).getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(e.getCause()).isNull();
                    return true;
                }).verify();
    }

    private Mono<StatisticsServiceProto.ListDataPointsRequest> stubListDataPointsRequest() {
        return Mono.just(StatisticsServiceProto.ListDataPointsRequest.newBuilder()
                .setAccountName(ACCOUNT_NAME)
                .build());
    }

    private ItemMetric grocery() {
        return ItemMetric.builder()
                .type(ItemType.EXPENSE)
                .title(GROCERY)
                .moneyAmount(GROCERY_AMOUNT)
                .build();
    }

    private ItemMetric vacation() {
        return ItemMetric.builder()
                .type(ItemType.EXPENSE)
                .title(VACATION)
                .moneyAmount(VACATION_AMOUNT)
                .build();
    }

    private ItemMetric salary() {
        return ItemMetric.builder()
                .type(ItemType.INCOME)
                .title(SALARY)
                .moneyAmount(SALARY_AMOUNT)
                .build();
    }

    private DataPoint stubDataPoint(LocalDate date, BigDecimal saving, ItemMetric... metrics) {
        var dataPoint = updateStatistics(ACCOUNT_NAME, ImmutableList.copyOf(metrics), saving);
        return DataPoint.builder()
                .accountName(ACCOUNT_NAME)
                .date(date)
                .metrics(dataPoint.getMetrics())
                .statistics(dataPoint.getStatistics())
                .build();
    }
}