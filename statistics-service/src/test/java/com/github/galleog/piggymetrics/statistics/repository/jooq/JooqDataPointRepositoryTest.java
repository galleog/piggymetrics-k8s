package com.github.galleog.piggymetrics.statistics.repository.jooq;

import static com.github.galleog.piggymetrics.statistics.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.INCOME;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.DATA_POINTS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.ITEM_METRICS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.STATISTICAL_METRICS;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.r2dbc.jooq.config.R2dbcJooqAutoConfiguration;
import com.github.galleog.r2dbc.jooq.transaction.TransactionAwareJooqWrapper;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.GregorianCalendar;

/**
 * Tests for {@link JooqDataPointRepository}.
 */
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
@Import(JooqDataPointRepositoryTest.DataSourceConfig.class)
@ImportAutoConfiguration(R2dbcJooqAutoConfiguration.class)
class JooqDataPointRepositoryTest {
    private static final String POSTGRES_IMAGE = "postgres:13.8-alpine";
    private static final String ACCOUNT_NAME = "test";
    private static final LocalDate NOW = LocalDate.now();
    private static final LocalDate DAY_BEFORE = NOW.minusDays(1);
    private static final long SALARY_ID = 100;
    private static final String SALARY = "Salary";
    private static final BigDecimal SALARY_AMOUNT = BigDecimal.valueOf(30000, 2);
    private static final BigDecimal ANOTHER_SALARY_AMOUNT = BigDecimal.valueOf(33000, 2);
    private static final long GROCERY_ID = 101;
    private static final String GROCERY = "Grocery";
    private static final BigDecimal GROCERY_AMOUNT = BigDecimal.valueOf(1000, 2);
    private static final long VACATION_ID = 102;
    private static final String VACATION = "Vacation";
    private static final BigDecimal VACATION_AMOUNT = BigDecimal.valueOf(11300, 2);
    private static final BigDecimal EXPENSES_AMOUNT = GROCERY_AMOUNT.add(VACATION_AMOUNT);
    private static final BigDecimal SAVING_AMOUNT = BigDecimal.valueOf(590000, 2);
    private static final BigDecimal ZERO = BigDecimal.valueOf(0, 2);

    private static final DbSetupTracker DB_SETUP_TRACKER = new DbSetupTracker();

    @Container
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private TransactionAwareJooqWrapper wrapper;

    private DataPointRepository repository;
    private DataSourceDestination destination;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> url("r2dbc"));
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.datasource.url", () -> url("jdbc"));
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
    }

    private static String url(String prefix) {
        return String.format("%s:postgresql://%s:%s/%s", prefix, postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @BeforeEach
    void setUp() {
        repository = new JooqDataPointRepository(wrapper);

        destination = DataSourceDestination.with(dataSource);
    }

    private DateValue toDateValue(LocalDate date) {
        return DateValue.from(GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class DataSourceConfig {
        @Bean
        DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                    .build();
        }
    }

    @Nested
    class ReadTest {
        @BeforeEach
        void setUp() {
            var operation = sequenceOf(
                    deleteAllFrom(
                            STATISTICAL_METRICS.getName(),
                            ITEM_METRICS.getName(),
                            DATA_POINTS.getName()
                    ),
                    insertInto(DATA_POINTS.getName())
                            .row()
                            .column(DATA_POINTS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(DATA_POINTS.DATA_POINT_DATE.getName(), NOW)
                            .end()
                            .row()
                            .column(DATA_POINTS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(DATA_POINTS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .end()
                            .build(),
                    insertInto(ITEM_METRICS.getName())
                            .row()
                            .column(ITEM_METRICS.ID.getName(), GROCERY_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), EXPENSE.name())
                            .column(ITEM_METRICS.TITLE.getName(), GROCERY)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), GROCERY_AMOUNT)
                            .end()
                            .row()
                            .column(ITEM_METRICS.ID.getName(), SALARY_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), ItemType.INCOME.name())
                            .column(ITEM_METRICS.TITLE.getName(), SALARY)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .end()
                            .row()
                            .column(ITEM_METRICS.ID.getName(), VACATION_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), EXPENSE.name())
                            .column(ITEM_METRICS.TITLE.getName(), VACATION)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), VACATION_AMOUNT)
                            .end()
                            .build(),
                    insertInto(STATISTICAL_METRICS.getName())
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.INCOMES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), ZERO)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.INCOMES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.EXPENSES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), EXPENSES_AMOUNT)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.EXPENSES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), ZERO)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.SAVING_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), SAVING_AMOUNT)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.SAVING_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), SAVING_AMOUNT)
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqDataPointRepository#getByAccountNameAndDate(String, LocalDate)}.
         */
        @Test
        void shouldGetDataPointByAccountNameAndDate() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByAccountNameAndDate(ACCOUNT_NAME, NOW)
                    .as(StepVerifier::create)
                    .expectNextMatches(dataPoint -> {
                        assertThat(dataPoint.getAccountName()).isEqualTo(ACCOUNT_NAME);
                        assertThat(dataPoint.getDate()).isEqualTo(NOW);
                        assertThat(dataPoint.getMetrics()).extracting(
                                ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                        ).containsExactlyInAnyOrder(
                                tuple(GROCERY_ID, EXPENSE, GROCERY, GROCERY_AMOUNT),
                                tuple(VACATION_ID, EXPENSE, VACATION, VACATION_AMOUNT)
                        );
                        assertThat(dataPoint.getStatistics()).containsOnly(
                                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ZERO),
                                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqDataPointRepository#getByAccountNameAndDate(String, LocalDate)}
         * when there is no data point with the specified date.
         */
        @Test
        void shouldNotGetDataPointByAccountNameAndDate() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByAccountNameAndDate(ACCOUNT_NAME, NOW.minusWeeks(1))
                    .as(StepVerifier::create)
                    .verifyComplete();
        }

        /**
         * Test for {@link JooqDataPointRepository#listByAccountName(String)}.
         */
        @Test
        void shouldListDataPointsByAccountName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.listByAccountName(ACCOUNT_NAME)
                    .as(StepVerifier::create)
                    .expectNextMatches(dataPoint -> {
                        assertThat(dataPoint.getAccountName()).isEqualTo(ACCOUNT_NAME);
                        assertThat(dataPoint.getDate()).isEqualTo(DAY_BEFORE);
                        assertThat(dataPoint.getMetrics()).extracting(
                                ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                        ).containsExactly(tuple(SALARY_ID, INCOME, SALARY, SALARY_AMOUNT));
                        assertThat(dataPoint.getStatistics()).containsOnly(
                                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, ZERO),
                                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                        );
                        return true;
                    }).expectNextMatches(dataPoint -> {
                        assertThat(dataPoint.getAccountName()).isEqualTo(ACCOUNT_NAME);
                        assertThat(dataPoint.getDate()).isEqualTo(NOW);
                        assertThat(dataPoint.getMetrics()).extracting(
                                ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                        ).containsExactlyInAnyOrder(
                                tuple(GROCERY_ID, EXPENSE, GROCERY, GROCERY_AMOUNT),
                                tuple(VACATION_ID, EXPENSE, VACATION, VACATION_AMOUNT)
                        );
                        assertThat(dataPoint.getStatistics()).containsOnly(
                                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ZERO),
                                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqDataPointRepository#listByAccountName(String)} when no data points are found.
         */
        @Test
        void shouldReturnEmptyStreamWhenNoDataPointsFound() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.listByAccountName("not found")
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            var operation = deleteAllFrom(
                    STATISTICAL_METRICS.getName(),
                    ITEM_METRICS.getName(),
                    DATA_POINTS.getName()
            );

            var dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqDataPointRepository#save(DataPoint)}.
         */
        @Test
        void shouldSaveDataPoint() {
            var grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(GROCERY_AMOUNT)
                    .build();
            var vacation = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(VACATION)
                    .moneyAmount(VACATION_AMOUNT)
                    .build();
            var salary = ItemMetric.builder()
                    .type(INCOME)
                    .title(SALARY)
                    .moneyAmount(SALARY_AMOUNT)
                    .build();
            var dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(NOW)
                    .metric(grocery)
                    .metric(vacation)
                    .metric(salary)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();

            repository.save(dataPoint)
                    .as(StepVerifier::create)
                    .expectNextMatches(dp -> {
                        var dataPoints = new Table(dataSource, DATA_POINTS.getName());
                        Assertions.assertThat(dataPoints)
                                .column(DATA_POINTS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_NAME)
                                .column(DATA_POINTS.DATA_POINT_DATE.getName()).containsValues(toDateValue(NOW));

                        var itemMetrics = new Table(dataSource, ITEM_METRICS.getName());
                        Assertions.assertThat(itemMetrics)
                                .column(ITEM_METRICS.TITLE.getName()).containsValues(GROCERY, VACATION, SALARY)
                                .column(ITEM_METRICS.MONEY_AMOUNT.getName()).containsValues(GROCERY_AMOUNT, VACATION_AMOUNT, SALARY_AMOUNT)
                                .column(ITEM_METRICS.ITEM_TYPE.getName()).containsValues(EXPENSE.name(), EXPENSE.name(), INCOME.name());

                        var statisticalMetrics = new Table(dataSource, STATISTICAL_METRICS.getName());
                        Assertions.assertThat(statisticalMetrics)
                                .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName())
                                .containsValues(
                                        StatisticalMetric.EXPENSES_AMOUNT.name(),
                                        StatisticalMetric.INCOMES_AMOUNT.name(),
                                        StatisticalMetric.SAVING_AMOUNT.name()
                                ).column(STATISTICAL_METRICS.MONEY_AMOUNT.getName())
                                .containsValues(EXPENSES_AMOUNT, SALARY_AMOUNT, SAVING_AMOUNT);

                        assertThat(dp.getAccountName()).isEqualTo(ACCOUNT_NAME);
                        assertThat(dp.getDate()).isEqualTo(NOW);
                        assertThat(dp.getMetrics()).extracting(
                                ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                        ).containsExactlyInAnyOrder(
                                tuple(EXPENSE, GROCERY, GROCERY_AMOUNT),
                                tuple(EXPENSE, VACATION, VACATION_AMOUNT),
                                tuple(INCOME, SALARY, SALARY_AMOUNT)
                        );
                        assertThat(dp.getStatistics()).containsOnly(
                                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                        );
                        return true;
                    }).verifyComplete();
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            var operation = sequenceOf(
                    deleteAllFrom(
                            STATISTICAL_METRICS.getName(),
                            ITEM_METRICS.getName(),
                            DATA_POINTS.getName()
                    ),
                    insertInto(DATA_POINTS.getName())
                            .row()
                            .column(DATA_POINTS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(DATA_POINTS.DATA_POINT_DATE.getName(), NOW)
                            .end()
                            .build(),
                    insertInto(ITEM_METRICS.getName())
                            .row()
                            .column(ITEM_METRICS.ID.getName(), SALARY_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), ItemType.INCOME.name())
                            .column(ITEM_METRICS.TITLE.getName(), SALARY)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .end()
                            .build(),
                    insertInto(STATISTICAL_METRICS.getName())
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.INCOMES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.EXPENSES_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), ZERO)
                            .end()
                            .row()
                            .column(STATISTICAL_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(STATISTICAL_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName(), StatisticalMetric.SAVING_AMOUNT.name())
                            .column(STATISTICAL_METRICS.MONEY_AMOUNT.getName(), ZERO)
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqDataPointRepository#update(DataPoint)}.
         */
        @Test
        void shouldUpdateDataPoint() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(GROCERY_AMOUNT)
                    .build();
            var vacation = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(VACATION)
                    .moneyAmount(VACATION_AMOUNT)
                    .build();
            var salary = ItemMetric.builder()
                    .type(INCOME)
                    .title(SALARY)
                    .moneyAmount(ANOTHER_SALARY_AMOUNT)
                    .build();
            var dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(NOW)
                    .metric(grocery)
                    .metric(vacation)
                    .metric(salary)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, ANOTHER_SALARY_AMOUNT)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();

            repository.update(dataPoint)
                    .as(StepVerifier::create)
                    .expectNextMatches(dp -> {
                        var dataPoints = new Table(dataSource, DATA_POINTS.getName());
                        Assertions.assertThat(dataPoints)
                                .column(DATA_POINTS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_NAME)
                                .column(DATA_POINTS.DATA_POINT_DATE.getName()).containsValues(toDateValue(NOW));

                        var itemMetrics = new Table(dataSource, ITEM_METRICS.getName());
                        Assertions.assertThat(itemMetrics)
                                .column(ITEM_METRICS.TITLE.getName()).containsValues(GROCERY, VACATION, SALARY)
                                .column(ITEM_METRICS.MONEY_AMOUNT.getName())
                                .containsValues(GROCERY_AMOUNT, VACATION_AMOUNT, ANOTHER_SALARY_AMOUNT)
                                .column(ITEM_METRICS.ITEM_TYPE.getName()).containsValues(EXPENSE.name(), EXPENSE.name(), INCOME.name());

                        var statisticalMetrics = new Table(dataSource, STATISTICAL_METRICS.getName());
                        Assertions.assertThat(statisticalMetrics)
                                .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName())
                                .containsValues(
                                        StatisticalMetric.EXPENSES_AMOUNT.name(),
                                        StatisticalMetric.INCOMES_AMOUNT.name(),
                                        StatisticalMetric.SAVING_AMOUNT.name()
                                ).column(STATISTICAL_METRICS.MONEY_AMOUNT.getName())
                                .containsValues(EXPENSES_AMOUNT, ANOTHER_SALARY_AMOUNT, SAVING_AMOUNT);

                        assertThat(dp.getAccountName()).isEqualTo(ACCOUNT_NAME);
                        assertThat(dp.getDate()).isEqualTo(NOW);
                        assertThat(dp.getMetrics()).extracting(
                                ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
                        ).containsExactlyInAnyOrder(
                                tuple(EXPENSE, GROCERY, GROCERY_AMOUNT),
                                tuple(EXPENSE, VACATION, VACATION_AMOUNT),
                                tuple(INCOME, SALARY, ANOTHER_SALARY_AMOUNT)
                        );
                        assertThat(dp.getStatistics()).containsOnly(
                                new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ANOTHER_SALARY_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                                new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqDataPointRepository#update(DataPoint)} when no data point exists with the specified date.
         */
        @Test
        void shouldNotUpdateDataPoint() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(DAY_BEFORE)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, ZERO)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, ZERO)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();

            repository.update(dataPoint)
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }
}
