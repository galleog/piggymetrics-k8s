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

import com.github.galleog.piggymetrics.statistics.config.JooqConfig;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.google.common.collect.ImmutableList;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tests for {@link JooqDataPointRepository}.
 */
@JooqTest
@ActiveProfiles("test")
@Import(JooqConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JooqDataPointRepositoryTest {
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

    @Autowired
    private DataSource dataSource;
    @Autowired
    private DSLContext dsl;

    private DataPointRepository repository;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        repository = new JooqDataPointRepository(dsl);

        destination = DataSourceDestination.with(dataSource);
    }

    private DateValue toDateValue(LocalDate date) {
        return DateValue.from(GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())));
    }

    @Nested
    class ReadTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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
                            .column(ITEM_METRICS.ID.getName(), SALARY_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), DAY_BEFORE)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), ItemType.INCOME.name())
                            .column(ITEM_METRICS.TITLE.getName(), SALARY)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .end()
                            .row()
                            .column(ITEM_METRICS.ID.getName(), GROCERY_ID)
                            .column(ITEM_METRICS.ACCOUNT_NAME.getName(), ACCOUNT_NAME)
                            .column(ITEM_METRICS.DATA_POINT_DATE.getName(), NOW)
                            .column(ITEM_METRICS.ITEM_TYPE.getName(), EXPENSE.name())
                            .column(ITEM_METRICS.TITLE.getName(), GROCERY)
                            .column(ITEM_METRICS.MONEY_AMOUNT.getName(), GROCERY_AMOUNT)
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqDataPointRepository#getByAccountNameAndDate(String, LocalDate)}.
         */
        @Test
        void shouldGetDataPointByAccountNameAndDate() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<DataPoint> dataPoint = repository.getByAccountNameAndDate(ACCOUNT_NAME, NOW);
            assertThat(dataPoint).isPresent();
            assertThat(dataPoint.get().getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(dataPoint.get().getDate()).isEqualTo(NOW);
            assertThat(dataPoint.get().getMetrics()).extracting(
                    ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, EXPENSE, GROCERY, GROCERY_AMOUNT),
                    tuple(VACATION_ID, EXPENSE, VACATION, VACATION_AMOUNT)
            );
            assertThat(dataPoint.get().getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ZERO),
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
            );
        }

        /**
         * Test for {@link JooqDataPointRepository#listByAccountName(String)}.
         */
        @Test
        void shouldListDataPointsByAccountName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<DataPoint> dataPoints;
            try (Stream<DataPoint> stream = repository.listByAccountName(ACCOUNT_NAME)) {
                dataPoints = stream.collect(ImmutableList.toImmutableList());
            }

            assertThat(dataPoints).extracting(DataPoint::getDate)
                    .containsExactlyInAnyOrder(NOW, DAY_BEFORE);

            DataPoint now = dataPoints.stream().filter(dataPoint -> NOW.equals(dataPoint.getDate()))
                    .findFirst()
                    .get();
            assertThat(now.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(now.getMetrics()).extracting(
                    ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, EXPENSE, GROCERY, GROCERY_AMOUNT),
                    tuple(VACATION_ID, EXPENSE, VACATION, VACATION_AMOUNT)
            );
            assertThat(now.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ZERO),
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
            );

            DataPoint dayBefore = dataPoints.stream().filter(dataPoint -> DAY_BEFORE.equals(dataPoint.getDate()))
                    .findFirst()
                    .get();
            assertThat(dayBefore.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(dayBefore.getMetrics()).extracting(
                    ItemMetric::getId, ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactly(tuple(SALARY_ID, INCOME, SALARY, SALARY_AMOUNT));
            assertThat(dayBefore.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, ZERO),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
            );
        }

        /**
         * Test for {@link JooqDataPointRepository#listByAccountName(String)} when no data points are found.
         */
        @Test
        void shouldReturnEmptySteamWhenNoDataPointsFound() {
            DB_SETUP_TRACKER.skipNextLaunch();

            try (Stream<DataPoint> stream = repository.listByAccountName("not found")) {
                assertThat(stream).isEmpty();
            }
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = deleteAllFrom(
                    STATISTICAL_METRICS.getName(),
                    ITEM_METRICS.getName(),
                    DATA_POINTS.getName()
            );

            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqDataPointRepository#save(DataPoint)}.
         */
        @Test
        void shouldSaveDataPoint() {
            ItemMetric grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(GROCERY_AMOUNT)
                    .build();
            ItemMetric vacation = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(VACATION)
                    .moneyAmount(VACATION_AMOUNT)
                    .build();
            ItemMetric salary = ItemMetric.builder()
                    .type(INCOME)
                    .title(SALARY)
                    .moneyAmount(SALARY_AMOUNT)
                    .build();
            DataPoint dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(NOW)
                    .metric(grocery)
                    .metric(vacation)
                    .metric(salary)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();

            DataPoint saved = repository.save(dataPoint);

            Table dataPoints = new Table(dataSource, DATA_POINTS.getName());
            Assertions.assertThat(dataPoints)
                    .column(DATA_POINTS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_NAME)
                    .column(DATA_POINTS.DATA_POINT_DATE.getName()).containsValues(toDateValue(NOW));

            Table itemMetrics = new Table(dataSource, ITEM_METRICS.getName());
            Assertions.assertThat(itemMetrics)
                    .column(ITEM_METRICS.TITLE.getName()).containsValues(GROCERY, VACATION, SALARY)
                    .column(ITEM_METRICS.MONEY_AMOUNT.getName()).containsValues(GROCERY_AMOUNT, VACATION_AMOUNT, SALARY_AMOUNT)
                    .column(ITEM_METRICS.ITEM_TYPE.getName()).containsValues(EXPENSE.name(), EXPENSE.name(), INCOME.name());

            Table statisticalMetrics = new Table(dataSource, STATISTICAL_METRICS.getName());
            Assertions.assertThat(statisticalMetrics)
                    .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName())
                    .containsValues(
                            StatisticalMetric.EXPENSES_AMOUNT.name(),
                            StatisticalMetric.INCOMES_AMOUNT.name(),
                            StatisticalMetric.SAVING_AMOUNT.name()
                    ).column(STATISTICAL_METRICS.MONEY_AMOUNT.getName())
                    .containsValues(EXPENSES_AMOUNT, SALARY_AMOUNT, SAVING_AMOUNT);

            assertThat(saved.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(saved.getDate()).isEqualTo(NOW);
            assertThat(saved.getMetrics()).extracting(
                    ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(EXPENSE, GROCERY, GROCERY_AMOUNT),
                    tuple(EXPENSE, VACATION, VACATION_AMOUNT),
                    tuple(INCOME, SALARY, SALARY_AMOUNT)
            );
            assertThat(saved.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, SALARY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
            );
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqDataPointRepository#update(DataPoint)}.
         */
        @Test
        void shouldUpdateDataPoint() {
            DB_SETUP_TRACKER.skipNextLaunch();

            ItemMetric grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(GROCERY_AMOUNT)
                    .build();
            ItemMetric vacation = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(VACATION)
                    .moneyAmount(VACATION_AMOUNT)
                    .build();
            ItemMetric salary = ItemMetric.builder()
                    .type(INCOME)
                    .title(SALARY)
                    .moneyAmount(ANOTHER_SALARY_AMOUNT)
                    .build();
            DataPoint dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(NOW)
                    .metric(grocery)
                    .metric(vacation)
                    .metric(salary)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, ANOTHER_SALARY_AMOUNT)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();

            DataPoint updated = repository.update(dataPoint).get();

            Table dataPoints = new Table(dataSource, DATA_POINTS.getName());
            Assertions.assertThat(dataPoints)
                    .column(DATA_POINTS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_NAME)
                    .column(DATA_POINTS.DATA_POINT_DATE.getName()).containsValues(toDateValue(NOW));

            Table itemMetrics = new Table(dataSource, ITEM_METRICS.getName());
            Assertions.assertThat(itemMetrics)
                    .column(ITEM_METRICS.TITLE.getName()).containsValues(GROCERY, VACATION, SALARY)
                    .column(ITEM_METRICS.MONEY_AMOUNT.getName())
                    .containsValues(GROCERY_AMOUNT, VACATION_AMOUNT, ANOTHER_SALARY_AMOUNT)
                    .column(ITEM_METRICS.ITEM_TYPE.getName()).containsValues(EXPENSE.name(), EXPENSE.name(), INCOME.name());

            Table statisticalMetrics = new Table(dataSource, STATISTICAL_METRICS.getName());
            Assertions.assertThat(statisticalMetrics)
                    .column(STATISTICAL_METRICS.STATISTICAL_METRIC.getName())
                    .containsValues(
                            StatisticalMetric.EXPENSES_AMOUNT.name(),
                            StatisticalMetric.INCOMES_AMOUNT.name(),
                            StatisticalMetric.SAVING_AMOUNT.name()
                    ).column(STATISTICAL_METRICS.MONEY_AMOUNT.getName())
                    .containsValues(EXPENSES_AMOUNT, ANOTHER_SALARY_AMOUNT, SAVING_AMOUNT);

            assertThat(updated.getAccountName()).isEqualTo(ACCOUNT_NAME);
            assertThat(updated.getDate()).isEqualTo(NOW);
            assertThat(updated.getMetrics()).extracting(
                    ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(EXPENSE, GROCERY, GROCERY_AMOUNT),
                    tuple(EXPENSE, VACATION, VACATION_AMOUNT),
                    tuple(INCOME, SALARY, ANOTHER_SALARY_AMOUNT)
            );
            assertThat(updated.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticalMetric.INCOMES_AMOUNT, ANOTHER_SALARY_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.EXPENSES_AMOUNT, EXPENSES_AMOUNT),
                    new SimpleEntry<>(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
            );
        }

        /**
         * Test for {@link JooqDataPointRepository#update(DataPoint)} when no data point exists with the specified date.
         */
        @Test
        void shouldNotUpdateDataPoint() {
            DB_SETUP_TRACKER.skipNextLaunch();

            DataPoint dataPoint = DataPoint.builder()
                    .accountName(ACCOUNT_NAME)
                    .date(DAY_BEFORE)
                    .statistic(StatisticalMetric.INCOMES_AMOUNT, ZERO)
                    .statistic(StatisticalMetric.EXPENSES_AMOUNT, ZERO)
                    .statistic(StatisticalMetric.SAVING_AMOUNT, SAVING_AMOUNT)
                    .build();
            assertThat(repository.update(dataPoint)).isNotPresent();
        }
    }
}
