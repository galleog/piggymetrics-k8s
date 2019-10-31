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

import com.github.galleog.piggymetrics.account.config.JooqConfig;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.javamoney.moneta.Money;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link JooqAccountRepository}.
 */
@JooqTest
@Testcontainers
@ActiveProfiles("test")
@Import(JooqConfig.class)
class JooqAccountRepositoryTest {
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

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DSLContext dsl;

    private AccountRepository repository;
    private TransactionTemplate transactionTemplate;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        repository = new JooqAccountRepository(dsl);

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

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
                .interest(BigDecimal.valueOf(1.7))
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

    @Nested
    class ReadTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there are some items.
         */
        @Test
        void shouldGetAccountByNameWithItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<Account> account = repository.getByName(ACCOUNT_1_NAME);
            assertThat(account).isPresent();
            assertThat(account.get().getName()).isEqualTo(ACCOUNT_1_NAME);
            assertThat(account.get().getNote()).isNull();
            assertThat(account.get().getUpdateTime()).isEqualTo(NOW);

            Saving saving = account.get().getSaving();
            assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, USD));
            assertThat(saving.getInterest()).isEqualTo(INTEREST);
            assertThat(saving.isDeposit()).isTrue();
            assertThat(saving.isCapitalization()).isFalse();

            assertThat(account.get().getItems()).extracting(
                    Item::getId, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, GROCERY, Money.of(GROCERY_AMOUNT, USD), DAY, GROCERY_ICON, EXPENSE),
                    tuple(VACATION_ID, VACATION, Money.of(VACATION_AMOUNT, EUR), MONTH, VACATION_ICON, EXPENSE),
                    tuple(SALARY_ID, SALARY, Money.of(SALARY_AMOUNT, USD), MONTH, SALARY_ICON, INCOME)
            );
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there are no items.
         */
        @Test
        void shouldGetAccountByNameWithoutItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<Account> account = repository.getByName(ACCOUNT_2_NAME);
            assertThat(account.get().getName()).isEqualTo(ACCOUNT_2_NAME);
            assertThat(account.get().getNote()).isEqualTo(NOTE);
            assertThat(account.get().getUpdateTime()).isEqualTo(NOW);

            Saving saving = account.get().getSaving();
            assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(ZERO, EUR));
            assertThat(saving.getInterest()).isEqualTo(ZERO);
            assertThat(saving.isDeposit()).isFalse();
            assertThat(saving.isCapitalization()).isFalse();

            assertThat(account.get().getItems()).isEmpty();
        }

        /**
         * Test for {@link JooqAccountRepository#getByName(String)} when there is no account with the specified name.
         */
        @Test
        void shouldNotGetAccountByName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            assertThat(repository.getByName("noname")).isNotPresent();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(
                            ITEMS.getName(),
                            SAVINGS.getName(),
                            ACCOUNTS.getName()
                    )
            );

            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqAccountRepository#save(Account)}.
         */
        @Test
        void shouldSaveAccount() {
            Item income = stubIncome();
            Item expense = stubExpense();
            Account account = stubAccount(ImmutableList.of(income, expense));
            Account saved = transactionTemplate.execute(status -> repository.save(account));

            Table accounts = new Table(dataSource, ACCOUNTS.getName());
            Assertions.assertThat(accounts)
                    .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues()
                    .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE);

            Table savings = new Table(dataSource, SAVINGS.getName());
            Assertions.assertThat(savings)
                    .column(SAVINGS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                    .column(SAVINGS.MONEY_AMOUNT.getName())
                    .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                    .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                    .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                    .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

            Table items = new Table(dataSource, ITEMS.getName());
            Assertions.assertThat(items)
                    .column(ITEMS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME, ACCOUNT_1_NAME)
                    .column(ITEMS.TITLE.getName()).containsValues(income.getTitle(), expense.getTitle())
                    .column(ITEMS.CURRENCY_CODE.getName()).containsValues(EUR, EUR)
                    .column(ITEMS.MONEY_AMOUNT.getName())
                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                    .column(ITEMS.PERIOD.getName()).containsValues(MONTH.name(), YEAR.name())
                    .column(ITEMS.ICON.getName()).containsValues(income.getIcon(), expense.getIcon())
                    .column(ITEMS.ITEM_TYPE.getName()).containsValues(INCOME.name(), EXPENSE.name());

            assertThat(saved.getName()).isEqualTo(ACCOUNT_1_NAME);
            assertThat(saved.getNote()).isEqualTo(NOTE);
            assertThat(saved.getUpdateTime()).isNotNull();

            assertThat(saved.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
            assertThat(saved.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
            assertThat(saved.getSaving().isDeposit()).isTrue();
            assertThat(saved.getSaving().isCapitalization()).isTrue();

            assertThat(saved.getItems()).extracting(Item::getId).doesNotContainNull();
            assertThat(saved.getItems()).extracting(
                    Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
            ).containsExactlyInAnyOrder(
                    tuple(income.getTitle(), income.getMoneyAmount(), MONTH, income.getIcon(), INCOME),
                    tuple(expense.getTitle(), expense.getMoneyAmount(), YEAR, expense.getIcon(), EXPENSE)
            );
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when the updated account has some items.
         */
        @Test
        void shouldUpdateToAccountWithItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Item income = stubIncome();
            Item expense = stubExpense();
            Account account = stubAccount(ImmutableList.of(income, expense));
            Account updated = transactionTemplate.execute(status -> repository.update(account).get());

            Table accounts = new Table(dataSource, ACCOUNTS.getName());
            Assertions.assertThat(accounts)
                    .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE)
                    .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues();

            Table savings = new Table(dataSource, SAVINGS.getName());
            Assertions.assertThat(savings)
                    .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                    .column(SAVINGS.MONEY_AMOUNT.getName())
                    .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                    .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                    .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                    .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

            Table items = new Table(dataSource, ITEMS.getName());
            Assertions.assertThat(items)
                    .column(ITEMS.TITLE.getName()).containsValues(income.getTitle(), expense.getTitle())
                    .column(ITEMS.CURRENCY_CODE.getName()).containsValues(EUR, EUR)
                    .column(ITEMS.MONEY_AMOUNT.getName())
                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                    .column(ITEMS.PERIOD.getName()).containsValues(MONTH.name(), YEAR.name())
                    .column(ITEMS.ICON.getName()).containsValues(income.getIcon(), expense.getIcon())
                    .column(ITEMS.ITEM_TYPE.getName()).containsValues(INCOME.name(), EXPENSE.name());

            assertThat(updated.getName()).isEqualTo(account.getName());
            assertThat(updated.getNote()).isEqualTo(account.getNote());
            assertThat(updated.getUpdateTime()).isAfter(NOW);

            assertThat(updated.getItems()).extracting(Item::getId).doesNotContainNull();
            assertThat(updated.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
            assertThat(updated.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
            assertThat(updated.getSaving().isDeposit()).isTrue();
            assertThat(updated.getSaving().isCapitalization()).isTrue();

            assertThat(updated.getItems()).extracting(
                    Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
            ).containsExactlyInAnyOrder(
                    tuple(income.getTitle(), income.getMoneyAmount(), MONTH, income.getIcon(), INCOME),
                    tuple(expense.getTitle(), expense.getMoneyAmount(), YEAR, expense.getIcon(), EXPENSE)
            );
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when the updated account has no items.
         */
        @Test
        void shouldUpdateToAccountWithoutItems() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Account account = stubAccount(ImmutableList.of());
            Account updated = transactionTemplate.execute(status -> repository.update(account).get());

            Table accounts = new Table(dataSource, ACCOUNTS.getName());
            Assertions.assertThat(accounts)
                    .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE)
                    .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues();

            Table savings = new Table(dataSource, SAVINGS.getName());
            Assertions.assertThat(savings)
                    .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(USD)
                    .column(SAVINGS.MONEY_AMOUNT.getName())
                    .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                    .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                    .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                    .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

            Table items = new Table(dataSource, ITEMS.getName());
            Assertions.assertThat(items).isEmpty();

            assertThat(updated.getName()).isEqualTo(account.getName());
            assertThat(updated.getNote()).isEqualTo(account.getNote());
            assertThat(updated.getUpdateTime()).isAfter(NOW);

            assertThat(updated.getSaving().getMoneyAmount()).isEqualTo(account.getSaving().getMoneyAmount());
            assertThat(updated.getSaving().getInterest()).isEqualTo(account.getSaving().getInterest());
            assertThat(updated.getSaving().isDeposit()).isTrue();
            assertThat(updated.getSaving().isCapitalization()).isTrue();

            assertThat(updated.getItems()).isEmpty();
        }

        /**
         * Test for {@link JooqAccountRepository#update(Account)} when no account with the specified name exists.
         */
        @Test
        void shouldNotUpdateAccount() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Account account = Account.builder()
                    .name(ACCOUNT_2_NAME)
                    .saving(stubSaving())
                    .build();
            assertThat(repository.update(account)).isNotPresent();
        }
    }
}
