package com.github.galleog.piggymetrics.statistics.repository;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.BASE_CURRENCY;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.INCOME;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.DataPointId;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticMetric;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import com.google.common.collect.ImmutableList;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link DataPointRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class DataPointRepositoryTest {
    private static final String ACCOUNT_NAME = "test";
    private static final LocalDate NOW = LocalDate.now();
    private static final LocalDate DAY_BEFORE = NOW.minusDays(1);
    private static final long SALARY_ID = 100;
    private static final String SALARY = "Salary";
    private static final double SALARY_AMOUNT = 300;
    private static final long GROCERY_ID = 101;
    private static final String GROCERY = "Grocery";
    private static final double GROCERY_AMOUNT = 10;
    private static final long VACATION_ID = 102;
    private static final String VACATION = "Vacation";
    private static final double VACATION_AMOUNT = 113;
    private static final double TOTAL_INCOMES_AMOUNT = SALARY_AMOUNT;
    private static final double TOTAL_EXPENSES_AMOUNT = GROCERY_AMOUNT + VACATION_AMOUNT;
    private static final double SAVING_AMOUNT = 5900;

    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer();
    private static final DbSetupTracker DB_SETUP_TRACKER = new DbSetupTracker();

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataPointRepository repository;
    @MockBean
    private ConversionService conversionService;
    private TransactionTemplate transactionTemplate;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        destination = DataSourceDestination.with(dataSource);

        when(conversionService.convert(any(Money.class), eq(BASE_CURRENCY)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
                            DataPoint.STATISTICS_TABLE_NAME,
                            ItemMetric.TABLE_NAME,
                            DataPoint.TABLE_NAME
                    ),
                    insertInto(DataPoint.TABLE_NAME)
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("version", 1)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", DAY_BEFORE)
                            .column("version", 1)
                            .end()
                            .build(),
                    insertInto(ItemMetric.TABLE_NAME)
                            .row()
                            .column("id", SALARY_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", DAY_BEFORE)
                            .column("title", SALARY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SALARY_AMOUNT)
                            .column("item_type", ItemType.INCOME.ordinal())
                            .end()
                            .row()
                            .column("id", GROCERY_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("title", GROCERY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", GROCERY_AMOUNT)
                            .column("item_type", EXPENSE.ordinal())
                            .end()
                            .row()
                            .column("id", VACATION_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("title", VACATION)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", VACATION_AMOUNT)
                            .column("item_type", EXPENSE.ordinal())
                            .end()
                            .build(),
                    insertInto(DataPoint.STATISTICS_TABLE_NAME)
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.INCOMES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", 0)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", DAY_BEFORE)
                            .column("statistic_metric", StatisticMetric.INCOMES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", TOTAL_INCOMES_AMOUNT)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.EXPENSES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", TOTAL_EXPENSES_AMOUNT)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", DAY_BEFORE)
                            .column("statistic_metric", StatisticMetric.EXPENSES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", 0)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.SAVING_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SAVING_AMOUNT)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", DAY_BEFORE)
                            .column("statistic_metric", StatisticMetric.SAVING_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SAVING_AMOUNT)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link DataPointRepository#findById(DataPointId)}.
         */
        @Test
        void shouldFindDataPointById() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<DataPoint> dataPoint = repository.findById(DataPointId.of(ACCOUNT_NAME, NOW));
            assertThat(dataPoint).isPresent();
            assertThat(dataPoint.get().getIncomes()).isEmpty();
            assertThat(dataPoint.get().getExpenses()).extracting(
                    ItemMetric::getId, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, GROCERY, Money.of(GROCERY_AMOUNT, BASE_CURRENCY)),
                    tuple(VACATION_ID, VACATION, Money.of(VACATION_AMOUNT, BASE_CURRENCY))
            );
            assertThat(dataPoint.get().getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticMetric.INCOMES_AMOUNT, Money.of(0, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.EXPENSES_AMOUNT, Money.of(TOTAL_EXPENSES_AMOUNT, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.SAVING_AMOUNT, Money.of(SAVING_AMOUNT, BASE_CURRENCY))
            );
        }

        /**
         * Test for {@link DataPointRepository#findByIdAccount(String)}.
         */
        @Test
        void shouldFindDataPointsByAccountName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<DataPoint> dataPoints = repository.findByIdAccount(ACCOUNT_NAME);
            assertThat(dataPoints).extracting(DataPoint::getDate)
                    .containsExactlyInAnyOrder(NOW, DAY_BEFORE);

            DataPoint now = dataPoints.stream().filter(dataPoint -> NOW.equals(dataPoint.getDate()))
                    .findFirst()
                    .get();
            assertThat(now.getIncomes()).isEmpty();
            assertThat(now.getExpenses()).extracting(
                    ItemMetric::getId, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, GROCERY, Money.of(GROCERY_AMOUNT, BASE_CURRENCY)),
                    tuple(VACATION_ID, VACATION, Money.of(VACATION_AMOUNT, BASE_CURRENCY))
            );
            assertThat(now.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticMetric.INCOMES_AMOUNT, Money.of(0, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.EXPENSES_AMOUNT, Money.of(TOTAL_EXPENSES_AMOUNT, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.SAVING_AMOUNT, Money.of(SAVING_AMOUNT, BASE_CURRENCY))
            );

            DataPoint dayBefore = dataPoints.stream().filter(dataPoint -> DAY_BEFORE.equals(dataPoint.getDate()))
                    .findFirst()
                    .get();
            assertThat(dayBefore.getIncomes()).extracting(
                    ItemMetric::getId, ItemMetric::getTitle, ItemMetric::getMoneyAmount
            ).containsExactly(tuple(SALARY_ID, SALARY, Money.of(SALARY_AMOUNT, BASE_CURRENCY)));
            assertThat(dayBefore.getExpenses()).isEmpty();
            assertThat(dayBefore.getStatistics()).containsOnly(
                    new SimpleEntry<>(StatisticMetric.INCOMES_AMOUNT, Money.of(TOTAL_INCOMES_AMOUNT, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.EXPENSES_AMOUNT, Money.of(0, BASE_CURRENCY)),
                    new SimpleEntry<>(StatisticMetric.SAVING_AMOUNT, Money.of(SAVING_AMOUNT, BASE_CURRENCY))
            );
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = deleteAllFrom(
                    DataPoint.STATISTICS_TABLE_NAME,
                    ItemMetric.TABLE_NAME,
                    DataPoint.TABLE_NAME
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link DataPointRepository#save(Object)}.
         */
        @Test
        void shouldSaveDataPoint() {
            ItemMetric grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(Money.of(GROCERY_AMOUNT, BASE_CURRENCY))
                    .build();
            ItemMetric vacation = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(VACATION)
                    .moneyAmount(Money.of(VACATION_AMOUNT, BASE_CURRENCY))
                    .build();
            ItemMetric salary = ItemMetric.builder()
                    .type(INCOME)
                    .title(SALARY)
                    .moneyAmount(Money.of(SALARY_AMOUNT, BASE_CURRENCY))
                    .build();
            Money saving = Money.of(SAVING_AMOUNT, BASE_CURRENCY);

            DataPoint dataPoint = DataPoint.builder()
                    .account(ACCOUNT_NAME)
                    .date(NOW)
                    .metric(grocery)
                    .metric(vacation)
                    .metric(salary)
                    .saving(saving)
                    .build();

            transactionTemplate.execute(status -> repository.save(dataPoint));

            Table datapoints = new Table(dataSource, DataPoint.TABLE_NAME);
            Assertions.assertThat(datapoints)
                    .column("account").containsValues(ACCOUNT_NAME)
                    .column("data_point_date").containsValues(toDateValue(NOW));

            Table metrics = new Table(dataSource, ItemMetric.TABLE_NAME);
            Assertions.assertThat(metrics)
                    .column("title").containsValues(GROCERY, VACATION, SALARY)
                    .column("currency_code")
                    .containsValues(BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode())
                    .column("amount").containsValues(GROCERY_AMOUNT, VACATION_AMOUNT, SALARY_AMOUNT)
                    .column("item_type").containsValues(EXPENSE.ordinal(), EXPENSE.ordinal(), INCOME.ordinal());

            Table statistics = new Table(dataSource, DataPoint.STATISTICS_TABLE_NAME);
            Assertions.assertThat(statistics)
                    .column("statistic_metric")
                    .containsValues(
                            StatisticMetric.EXPENSES_AMOUNT.ordinal(),
                            StatisticMetric.INCOMES_AMOUNT.ordinal(),
                            StatisticMetric.SAVING_AMOUNT.ordinal()
                    ).column("currency_code")
                    .containsValues(BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode())
                    .column("amount")
                    .containsValues(TOTAL_EXPENSES_AMOUNT, TOTAL_INCOMES_AMOUNT, SAVING_AMOUNT);
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(
                            DataPoint.STATISTICS_TABLE_NAME,
                            ItemMetric.TABLE_NAME,
                            DataPoint.TABLE_NAME
                    ),
                    insertInto(DataPoint.TABLE_NAME)
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("version", 1)
                            .end()
                            .build(),
                    insertInto(ItemMetric.TABLE_NAME)
                            .row()
                            .column("id", SALARY_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("title", SALARY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SALARY_AMOUNT)
                            .column("item_type", ItemType.INCOME.ordinal())
                            .end()
                            .row()
                            .column("id", GROCERY_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("title", GROCERY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", GROCERY_AMOUNT)
                            .column("item_type", EXPENSE.ordinal())
                            .end()
                            .row()
                            .column("id", VACATION_ID)
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("title", VACATION)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", VACATION_AMOUNT)
                            .column("item_type", EXPENSE.ordinal())
                            .end()
                            .build(),
                    insertInto(DataPoint.STATISTICS_TABLE_NAME)
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.INCOMES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", TOTAL_INCOMES_AMOUNT)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.EXPENSES_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", TOTAL_EXPENSES_AMOUNT)
                            .end()
                            .row()
                            .column("account", ACCOUNT_NAME)
                            .column("data_point_date", NOW)
                            .column("statistic_metric", StatisticMetric.SAVING_AMOUNT.ordinal())
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SAVING_AMOUNT)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for modification of an existing data point.
         */
        @Test
        void shouldUpdateDataPoint() {
            ItemMetric grocery = ItemMetric.builder()
                    .type(EXPENSE)
                    .title(GROCERY)
                    .moneyAmount(Money.of(GROCERY_AMOUNT, BASE_CURRENCY))
                    .build();

            transactionTemplate.execute(status -> {
                Optional<DataPoint> optional = repository.findById(DataPointId.of(ACCOUNT_NAME, NOW));
                assertThat(optional).isPresent();

                DataPoint dataPoint = optional.get();
                dataPoint.update(ImmutableList.of(grocery), Money.of(0, BASE_CURRENCY));
                repository.save(dataPoint);
                return null;
            });

            Table datapoints = new Table(dataSource, DataPoint.TABLE_NAME);
            Assertions.assertThat(datapoints)
                    .column("account").containsValues(ACCOUNT_NAME)
                    .column("data_point_date").containsValues(toDateValue(NOW));

            Table metrics = new Table(dataSource, ItemMetric.TABLE_NAME);
            Assertions.assertThat(metrics)
                    .column("title").containsValues(GROCERY)
                    .column("currency_code").containsValues(BASE_CURRENCY.getCurrencyCode())
                    .column("amount").containsValues(GROCERY_AMOUNT)
                    .column("item_type").containsValues(EXPENSE.ordinal());

            Table statistics = new Table(dataSource, DataPoint.STATISTICS_TABLE_NAME);
            Assertions.assertThat(statistics)
                    .column("statistic_metric")
                    .containsValues(
                            StatisticMetric.EXPENSES_AMOUNT.ordinal(),
                            StatisticMetric.INCOMES_AMOUNT.ordinal(),
                            StatisticMetric.SAVING_AMOUNT.ordinal()
                    ).column("currency_code")
                    .containsValues(BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode(), BASE_CURRENCY.getCurrencyCode())
                    .column("amount")
                    .containsValues(GROCERY_AMOUNT, 0, 0);
        }
    }
}
