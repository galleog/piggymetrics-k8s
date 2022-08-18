package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.config.RouterConfig.DEMO_ACCOUNT;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;

import com.github.galleog.piggymetrics.apigateway.model.statistics.DataPoint;
import com.github.galleog.piggymetrics.apigateway.model.statistics.ItemMetric;
import com.github.galleog.piggymetrics.apigateway.model.statistics.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.grpc.ReactorStatisticsServiceGrpc.StatisticsServiceImplBase;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto.ItemType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

/**
 * Tests for routing statistics requests.
 */
public class StatisticsRequestRouterTest extends BaseRouterTest {
    private static final String ACCOUNT_NAME = "test";
    private static final LocalDate DAY_AGO = LocalDate.now().minusDays(1);
    private static final LocalDate WEEK_AGO = LocalDate.now().minusWeeks(1);
    private static final String GROCERY = "Grocery";
    private static final BigDecimal GROCERY_AMOUNT = BigDecimal.valueOf(100, 1);
    private static final String RENT = "Rent";
    private static final BigDecimal RENT_AMOUNT = BigDecimal.valueOf(200, 1);
    private static final String SALARY = "Salary";
    private static final BigDecimal SALARY_AMOUNT = BigDecimal.valueOf(3000, 1);
    private static final BigDecimal SAVING_AMOUNT = BigDecimal.valueOf(59000, 1);

    @Captor
    private ArgumentCaptor<Mono<StatisticsServiceProto.ListDataPointsRequest>> requestCaptor;

    private StatisticsServiceImplBase statisticsService;

    @BeforeEach
    void setUp() throws Exception {
        statisticsService = spyGrpcService(StatisticsServiceImplBase.class, StatisticsHandler.STATISTICS_SERVICE);
    }

    /**
     * Test for GET /statistics/current.
     */
    @Test
    void shouldGetStatisticsForCurrentUser() {
        doReturn(Flux.just(
                stubDataPointProto(ACCOUNT_NAME, DAY_AGO, ImmutableList.of(grocery(), salary()), SAVING_AMOUNT)
        )).when(statisticsService).listDataPoints(requestCaptor.capture());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .get()
                .uri("/statistics/current")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(DataPoint.class)
                .value(list -> {
                    assertThat(list).extracting(DataPoint::getAccountName, DataPoint::getDate)
                            .containsExactlyInAnyOrder(tuple(ACCOUNT_NAME, DAY_AGO));

                    DataPoint dataPoint = list.get(0);
                    assertThat(dataPoint.getMetrics()).extracting(
                            ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                    ).containsExactlyInAnyOrder(
                            tuple(ItemType.EXPENSE, GROCERY, GROCERY_AMOUNT),
                            tuple(ItemType.INCOME, SALARY, SALARY_AMOUNT)
                    );
                    assertThat(dataPoint.getStatistics()).containsOnly(
                            new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, GROCERY_AMOUNT),
                            new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                            new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    );
                });

        requestCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(req -> ACCOUNT_NAME.equals(req.getAccountName()))
                .verifyComplete();
    }

    /**
     * Test for GET /statistics/current without authentication.
     */
    @Test
    void shouldFailToGetStatisticsForCurrentUserWithoutAuthentication() {
        webClient.get()
                .uri("/statistics/current")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test for GET /statistics/demo.
     */
    @Test
    public void shouldGetStatisticsForDemoAccount() {
        doReturn(Flux.just(
                stubDataPointProto(DEMO_ACCOUNT, DAY_AGO, ImmutableList.of(grocery(), rent(), salary()), SAVING_AMOUNT),
                stubDataPointProto(DEMO_ACCOUNT, WEEK_AGO, ImmutableList.of(salary()), BigDecimal.ZERO)
        )).when(statisticsService).listDataPoints(requestCaptor.capture());

        webClient.get()
                .uri("/statistics/demo")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(DataPoint.class)
                .value(list -> {
                    assertThat(list).extracting(DataPoint::getAccountName, DataPoint::getDate)
                            .containsExactlyInAnyOrder(tuple(DEMO_ACCOUNT, DAY_AGO), tuple(DEMO_ACCOUNT, WEEK_AGO));

                    DataPoint dayAgo = list.stream()
                            .filter(dataPoint -> DAY_AGO.equals(dataPoint.getDate()))
                            .findFirst().get();
                    assertThat(dayAgo.getMetrics()).extracting(
                            ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                    ).containsExactlyInAnyOrder(
                            tuple(ItemType.EXPENSE, GROCERY, GROCERY_AMOUNT),
                            tuple(ItemType.EXPENSE, RENT, RENT_AMOUNT),
                            tuple(ItemType.INCOME, SALARY, SALARY_AMOUNT)
                    );
                    assertThat(dayAgo.getStatistics()).containsOnly(
                            new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, GROCERY_AMOUNT.add(RENT_AMOUNT)),
                            new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                            new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    );

                    DataPoint weekAgo = list.stream()
                            .filter(dataPoint -> WEEK_AGO.equals(dataPoint.getDate()))
                            .findFirst().get();
                    assertThat(weekAgo.getMetrics()).extracting(
                            ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                    ).containsExactly(
                            tuple(ItemType.INCOME, SALARY, SALARY_AMOUNT)
                    );
                    assertThat(weekAgo.getStatistics()).containsOnly(
                            new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, BigDecimal.ZERO),
                            new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                            new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, BigDecimal.ZERO)
                    );
                });

        requestCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(req -> DEMO_ACCOUNT.equals(req.getAccountName()))
                .verifyComplete();
    }

    private StatisticsServiceProto.ItemMetric grocery() {
        return StatisticsServiceProto.ItemMetric.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle(GROCERY)
                .setMoneyAmount(bigDecimalConverter().convert(GROCERY_AMOUNT))
                .build();
    }

    private StatisticsServiceProto.ItemMetric rent() {
        return StatisticsServiceProto.ItemMetric.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle(RENT)
                .setMoneyAmount(bigDecimalConverter().convert(RENT_AMOUNT))
                .build();
    }

    private StatisticsServiceProto.ItemMetric salary() {
        return StatisticsServiceProto.ItemMetric.newBuilder()
                .setType(ItemType.INCOME)
                .setTitle(SALARY)
                .setMoneyAmount(bigDecimalConverter().convert(SALARY_AMOUNT))
                .build();
    }

    private StatisticsServiceProto.DataPoint stubDataPointProto(String accountName, LocalDate date,
                                                                List<StatisticsServiceProto.ItemMetric> metrics,
                                                                BigDecimal saving) {
        return StatisticsServiceProto.DataPoint.newBuilder()
                .setAccountName(accountName)
                .setDate(dateConverter().convert(date))
                .addAllMetrics(metrics)
                .putAllStatistics(ImmutableMap.of(
                        StatisticalMetric.INCOMES_AMOUNT.name(),
                        bigDecimalConverter().convert(
                                metrics.stream()
                                        .filter(metric -> ItemType.INCOME.equals(metric.getType()))
                                        .map(metric -> bigDecimalConverter().reverse().convert(metric.getMoneyAmount()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        ),
                        StatisticalMetric.EXPENSES_AMOUNT.name(),
                        bigDecimalConverter().convert(
                                metrics.stream()
                                        .filter(metric -> ItemType.EXPENSE.equals(metric.getType()))
                                        .map(metric -> bigDecimalConverter().reverse().convert(metric.getMoneyAmount()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        ),
                        StatisticalMetric.SAVING_AMOUNT.name(),
                        bigDecimalConverter().convert(saving)
                )).build();
    }
}