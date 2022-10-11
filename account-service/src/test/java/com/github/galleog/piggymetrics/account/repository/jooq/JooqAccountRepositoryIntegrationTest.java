package com.github.galleog.piggymetrics.account.repository.jooq;

import static com.github.galleog.piggymetrics.account.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.account.domain.ItemType.INCOME;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.YEAR;
import static com.github.galleog.piggymetrics.account.domain.tables.Accounts.ACCOUNTS;
import static com.github.galleog.piggymetrics.account.domain.tables.Items.ITEMS;
import static com.github.galleog.piggymetrics.account.domain.tables.Savings.SAVINGS;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.autoconfigure.jooq.R2dbcJooqAutoConfiguration;
import com.github.galleog.piggymetrics.autoconfigure.jooq.TransactionAwareJooqWrapper;
import com.google.common.collect.ImmutableList;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Integration tests for {@link JooqAccountRepository}.
 */
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
@Import(JooqAccountRepositoryIntegrationTest.DataSourceConfig.class)
@ImportAutoConfiguration(R2dbcJooqAutoConfiguration.class)
class JooqAccountRepositoryIntegrationTest {
    private static final String POSTGRES_IMAGE = "postgres:13.8-alpine";
    private static final String ACCOUNT_1_NAME = "test1";
    private static final String ACCOUNT_2_NAME = "test2";
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final double SAVING_AMOUNT = 5900;
    private static final BigDecimal INTEREST = BigDecimal.valueOf(3.32);
    private static final BigDecimal ZERO = BigDecimal.valueOf(0, 2);
    private static final String USD = "USD";
    private static final String EUR = "EUR";
    private static final String GROCERY = "Grocery";
    private static final long GROCERY_ID = 1;
    private static final double GROCERY_AMOUNT = 10;
    private static final String GROCERY_ICON = "meal";
    private static final String VACATION = "Vacation";
    private static final long VACATION_ID = 2;
    private static final double VACATION_AMOUNT = 3400;
    private static final String VACATION_ICON = "tourism";
    private static final String SALARY = "Salary";
    private static final long SALARY_ID = 3;
    private static final double SALARY_AMOUNT = 9100;
    private static final String SALARY_ICON = "wallet";
    private static final String NOTE = "note";

    private static final DbSetupTracker DB_SETUP_TRACKER = new DbSetupTracker();

    @Container
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private TransactionAwareJooqWrapper wrapper;
    private AccountRepository repository;
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
        repository = new JooqAccountRepository(wrapper);

        destination = DataSourceDestination.with(dataSource);
    }

    private Item stubExpense() {
        return Item.builder()
                .title("Rent")
                .moneyAmount(Money.of(12000, EUR))
                .period(YEAR)
                .icon("home")
                .type(EXPENSE)
                .build();
    }

    private Item stubIncome() {
        return Item.builder()
                .title("Salary")
                .moneyAmount(Money.of(7500, EUR))
                .period(MONTH)
                .icon("wallet")
                .type(INCOME)
                .build();
    }

    private Saving stubSaving() {
        return Saving.builder()
                .moneyAmount(Money.of(76000, USD))
                .interest(BigDecimal.valueOf(1.72))
                .deposit(true)
                .capitalization(true)
                .build();
    }

    private Account stubAccount(List<Item> items) {
        return Account.builder()
                .name(ACCOUNT_1_NAME)
                .items(items)
                .saving(stubSaving())
                .note(NOTE)
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class DataSourceConfig {
        @Bean
        @LiquibaseDataSource
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
                            ITEMS.getName(),
                            SAVINGS.getName(),
                            ACCOUNTS.getName()
                    ),
                    insertInto(ACCOUNTS.getName())
                            .row()
                            .column(ACCOUNTS.NAME.getName(), ACCOUNT_1_NAME)
                            .column(ACCOUNTS.NOTE.getName(), null)
                            .column(ACCOUNTS.UPDATE_TIME.getName(), NOW)
                            .end()
                            .row()
                            .column(ACCOUNTS.NAME.getName(), ACCOUNT_2_NAME)
                            .column(ACCOUNTS.NOTE.getName(), NOTE)
                            .column(ACCOUNTS.UPDATE_TIME.getName(), NOW)
                            .end()
                            .build(),
                    insertInto(SAVINGS.getName())
                            .row()
                            .column(SAVINGS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(SAVINGS.CURRENCY_CODE.getName(), USD)
                            .column(SAVINGS.MONEY_AMOUNT.getName(), SAVING_AMOUNT)
                            .column(SAVINGS.INTEREST.getName(), INTEREST)
                            .column(SAVINGS.DEPOSIT.getName(), true)
                            .column(SAVINGS.CAPITALIZATION.getName(), false)
                            .end()
                            .row()
                            .column(SAVINGS.ACCOUNT_NAME.getName(), ACCOUNT_2_NAME)
                            .column(SAVINGS.CURRENCY_CODE.getName(), EUR)
                            .column(SAVINGS.MONEY_AMOUNT.getName(), ZERO)
                            .column(SAVINGS.INTEREST.getName(), ZERO)
                            .column(SAVINGS.DEPOSIT.getName(), false)
                            .column(SAVINGS.CAPITALIZATION.getName(), false)
                            .end()
                            .build(),
                    insertInto(ITEMS.getName())
                            .row()
                            .column(ITEMS.ID.getName(), GROCERY_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), GROCERY)
                            .column(ITEMS.CURRENCY_CODE.getName(), USD)
                            .column(ITEMS.MONEY_AMOUNT.getName(), GROCERY_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), DAY.name())
                            .column(ITEMS.ICON.getName(), GROCERY_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), EXPENSE.name())
                            .end()
                            .row()
                            .column(ITEMS.ID.getName(), VACATION_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), VACATION)
                            .column(ITEMS.CURRENCY_CODE.getName(), EUR)
                            .column(ITEMS.MONEY_AMOUNT.getName(), VACATION_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), MONTH.name())
                            .column(ITEMS.ICON.getName(), VACATION_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), EXPENSE.name())
                            .end()
                            .row()
                            .column(ITEMS.ID.getName(), SALARY_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), SALARY)
                            .column(ITEMS.CURRENCY_CODE.getName(), USD)
                            .column(ITEMS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), MONTH.name())
                            .column(ITEMS.ICON.getName(), SALARY_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), INCOME.name())
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there are some items.
         */
        @Test
        void shouldGetAccountByNameWithItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByName(ACCOUNT_1_NAME)
                    .as(StepVerifier::create)
                    .expectNextMatches(a -> {
                        assertThat(a.getName()).isEqualTo(ACCOUNT_1_NAME);
                        assertThat(a.getNote()).isNull();
                        assertThat(a.getUpdateTime()).isEqualTo(NOW);

                        var saving = a.getSaving();
                        assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, USD));
                        assertThat(saving.getInterest()).isEqualTo(INTEREST);
                        assertThat(saving.isDeposit()).isTrue();
                        assertThat(saving.isCapitalization()).isFalse();

                        assertThat(a.getItems()).extracting(
                                Item::getId, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
                        ).containsExactlyInAnyOrder(
                                tuple(GROCERY_ID, GROCERY, Money.of(GROCERY_AMOUNT, USD), DAY, GROCERY_ICON, EXPENSE),
                                tuple(VACATION_ID, VACATION, Money.of(VACATION_AMOUNT, EUR), MONTH, VACATION_ICON, EXPENSE),
                                tuple(SALARY_ID, SALARY, Money.of(SALARY_AMOUNT, USD), MONTH, SALARY_ICON, INCOME)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there are no items.
         */
        @Test
        void shouldGetAccountByNameWithoutItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByName(ACCOUNT_2_NAME)
                    .as(StepVerifier::create)
                    .expectNextMatches(a -> {
                        assertThat(a.getName()).isEqualTo(ACCOUNT_2_NAME);
                        assertThat(a.getNote()).isEqualTo(NOTE);
                        assertThat(a.getUpdateTime()).isEqualTo(NOW);

                        var saving = a.getSaving();
                        assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(ZERO, EUR));
                        assertThat(saving.getInterest()).isEqualTo(ZERO);
                        assertThat(saving.isDeposit()).isFalse();
                        assertThat(saving.isCapitalization()).isFalse();

                        assertThat(a.getItems()).isEmpty();
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there is no account with the specified name.
         */
        @Test
        void shouldNotGetAccountByName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByName("noname")
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            var operation = sequenceOf(
                    deleteAllFrom(
                            ITEMS.getName(),
                            SAVINGS.getName(),
                            ACCOUNTS.getName()
                    )
            );

            var dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqAccountRepository#save(Account)}.
         */
        @Test
        void shouldSaveAccount() {
            var income = stubIncome();
            var expense = stubExpense();
            var account = stubAccount(ImmutableList.of(income, expense));

            repository.save(account)
                    .as(StepVerifier::create)
                    .expectNextMatches(a -> {
                        var accounts = new Table(dataSource, ACCOUNTS.getName());
                        Assertions.assertThat(accounts)
                                .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                                .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues()
                                .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE);

                        var savings = new Table(dataSource, SAVINGS.getName());
                        Assertions.assertThat(savings)
                                .column(SAVINGS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME)
                                .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                                .column(SAVINGS.MONEY_AMOUNT.getName())
                                .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                                .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                                .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                                .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

                        var items = new Table(dataSource, ITEMS.getName());
                        Assertions.assertThat(items)
                                .column(ITEMS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME, ACCOUNT_1_NAME)
                                .column(ITEMS.TITLE.getName()).containsValues(income.getTitle(), expense.getTitle())
                                .column(ITEMS.CURRENCY_CODE.getName()).containsValues(EUR, EUR)
                                .column(ITEMS.MONEY_AMOUNT.getName())
                                .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                                .column(ITEMS.PERIOD.getName()).containsValues(MONTH.name(), YEAR.name())
                                .column(ITEMS.ICON.getName()).containsValues(income.getIcon(), expense.getIcon())
                                .column(ITEMS.ITEM_TYPE.getName()).containsValues(INCOME.name(), EXPENSE.name());

                        assertThat(a.getName()).isEqualTo(ACCOUNT_1_NAME);
                        assertThat(a.getNote()).isEqualTo(NOTE);
                        assertThat(a.getUpdateTime()).isNotNull();

                        assertThat(a.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
                        assertThat(a.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
                        assertThat(a.getSaving().isDeposit()).isTrue();
                        assertThat(a.getSaving().isCapitalization()).isTrue();

                        assertThat(a.getItems()).extracting(Item::getId).doesNotContainNull();
                        assertThat(a.getItems()).extracting(
                                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
                        ).containsExactlyInAnyOrder(
                                tuple(income.getTitle(), income.getMoneyAmount(), MONTH, income.getIcon(), INCOME),
                                tuple(expense.getTitle(), expense.getMoneyAmount(), YEAR, expense.getIcon(), EXPENSE)
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
                            ITEMS.getName(),
                            SAVINGS.getName(),
                            ACCOUNTS.getName()
                    ),
                    insertInto(ACCOUNTS.getName())
                            .row()
                            .column(ACCOUNTS.NAME.getName(), ACCOUNT_1_NAME)
                            .column(ACCOUNTS.NOTE.getName(), null)
                            .column(ACCOUNTS.UPDATE_TIME.getName(), NOW)
                            .end()
                            .build(),
                    insertInto(SAVINGS.getName())
                            .row()
                            .column(SAVINGS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(SAVINGS.CURRENCY_CODE.getName(), USD)
                            .column(SAVINGS.MONEY_AMOUNT.getName(), SAVING_AMOUNT)
                            .column(SAVINGS.INTEREST.getName(), INTEREST)
                            .column(SAVINGS.DEPOSIT.getName(), true)
                            .column(SAVINGS.CAPITALIZATION.getName(), false)
                            .end()
                            .build(),
                    insertInto(ITEMS.getName())
                            .row()
                            .column(ITEMS.ID.getName(), GROCERY_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), GROCERY)
                            .column(ITEMS.CURRENCY_CODE.getName(), USD)
                            .column(ITEMS.MONEY_AMOUNT.getName(), GROCERY_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), DAY.name())
                            .column(ITEMS.ICON.getName(), GROCERY_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), EXPENSE.name())
                            .end()
                            .row()
                            .column(ITEMS.ID.getName(), VACATION_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), VACATION)
                            .column(ITEMS.CURRENCY_CODE.getName(), EUR)
                            .column(ITEMS.MONEY_AMOUNT.getName(), VACATION_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), MONTH.name())
                            .column(ITEMS.ICON.getName(), VACATION_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), EXPENSE.name())
                            .end()
                            .row()
                            .column(ITEMS.ID.getName(), SALARY_ID)
                            .column(ITEMS.ACCOUNT_NAME.getName(), ACCOUNT_1_NAME)
                            .column(ITEMS.TITLE.getName(), SALARY)
                            .column(ITEMS.CURRENCY_CODE.getName(), USD)
                            .column(ITEMS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), MONTH.name())
                            .column(ITEMS.ICON.getName(), SALARY_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), INCOME.name())
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when the updated account has some items.
         */
        @Test
        void shouldUpdateToAccountWithItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var income = stubIncome();
            var expense = stubExpense();
            var account = stubAccount(ImmutableList.of(income, expense));

            repository.update(account)
                    .as(StepVerifier::create)
                    .expectNextMatches(a -> {
                        var accounts = new Table(dataSource, ACCOUNTS.getName());
                        Assertions.assertThat(accounts)
                                .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                                .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE)
                                .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues();

                        var savings = new Table(dataSource, SAVINGS.getName());
                        Assertions.assertThat(savings)
                                .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                                .column(SAVINGS.MONEY_AMOUNT.getName())
                                .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                                .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                                .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                                .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

                        var items = new Table(dataSource, ITEMS.getName());
                        Assertions.assertThat(items)
                                .column(ITEMS.TITLE.getName()).containsValues(income.getTitle(), expense.getTitle())
                                .column(ITEMS.CURRENCY_CODE.getName()).containsValues(EUR, EUR)
                                .column(ITEMS.MONEY_AMOUNT.getName())
                                .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                                .column(ITEMS.PERIOD.getName()).containsValues(MONTH.name(), YEAR.name())
                                .column(ITEMS.ICON.getName()).containsValues(income.getIcon(), expense.getIcon())
                                .column(ITEMS.ITEM_TYPE.getName()).containsValues(INCOME.name(), EXPENSE.name());

                        assertThat(a.getName()).isEqualTo(account.getName());
                        assertThat(a.getNote()).isEqualTo(account.getNote());
                        assertThat(a.getUpdateTime()).isAfter(NOW);

                        assertThat(a.getItems()).extracting(Item::getId).doesNotContainNull();
                        assertThat(a.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
                        assertThat(a.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
                        assertThat(a.getSaving().isDeposit()).isTrue();
                        assertThat(a.getSaving().isCapitalization()).isTrue();

                        assertThat(a.getItems()).extracting(
                                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
                        ).containsExactlyInAnyOrder(
                                tuple(income.getTitle(), income.getMoneyAmount(), MONTH, income.getIcon(), INCOME),
                                tuple(expense.getTitle(), expense.getMoneyAmount(), YEAR, expense.getIcon(), EXPENSE)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when the updated account has no items.
         */
        @Test
        void shouldUpdateToAccountWithoutItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var account = stubAccount(ImmutableList.of());

            repository.update(account)
                    .as(StepVerifier::create)
                    .expectNextMatches(a -> {
                        var accounts = new Table(dataSource, ACCOUNTS.getName());
                        Assertions.assertThat(accounts)
                                .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                                .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE)
                                .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues();

                        var savings = new Table(dataSource, SAVINGS.getName());
                        Assertions.assertThat(savings)
                                .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                                .column(SAVINGS.MONEY_AMOUNT.getName())
                                .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                                .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                                .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                                .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

                        var items = new Table(dataSource, ITEMS.getName());
                        Assertions.assertThat(items).isEmpty();

                        assertThat(a.getName()).isEqualTo(account.getName());
                        assertThat(a.getNote()).isEqualTo(account.getNote());
                        assertThat(a.getUpdateTime()).isAfter(NOW);

                        assertThat(a.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
                        assertThat(a.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
                        assertThat(a.getSaving().isDeposit()).isTrue();
                        assertThat(a.getSaving().isCapitalization()).isTrue();

                        assertThat(a.getItems()).isEmpty();
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when no account with the specified name exists.
         */
        @Test
        void shouldNotUpdateAccount() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var account = Account.builder()
                    .name(ACCOUNT_2_NAME)
                    .saving(stubSaving())
                    .build();

            repository.update(account)
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }
}
