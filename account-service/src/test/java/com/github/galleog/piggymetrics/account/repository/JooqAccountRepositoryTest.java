package com.github.galleog.piggymetrics.account.repository;

import static com.github.galleog.piggymetrics.account.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.account.domain.ItemType.INCOME;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.YEAR;
import static com.github.galleog.piggymetrics.account.domain.tables.Accounts.ACCOUNTS;
import static com.github.galleog.piggymetrics.account.domain.tables.Items.ITEMS;
import static com.github.galleog.piggymetrics.account.domain.tables.Savings.SAVINGS;
import static com.github.galleog.piggymetrics.account.service.AccountService.BASE_CURRENCY;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tests for {@link JooqAccountRepository}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
class JooqAccountRepositoryTest {
    private static final String ACCOUNT_1_NAME = "test1";
    private static final String ACCOUNT_2_NAME = "test2";
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final double SAVING_AMOUNT = 5900;
    private static final BigDecimal INTEREST = BigDecimal.valueOf(3.32);
    private static final BigDecimal ZERO = BigDecimal.valueOf(0, 2);
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

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AccountRepository repository;
    private TransactionTemplate transactionTemplate;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
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
                .moneyAmount(Money.of(76000, BASE_CURRENCY))
                .interest(BigDecimal.valueOf(1.7))
                .deposit(true)
                .capitalization(true)
                .build();
    }

    private Account stubAccount() {
        return Account.builder()
                .name(ACCOUNT_1_NAME)
                .item(stubIncome())
                .item(stubExpense())
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
                            .column(SAVINGS.CURRENCY_CODE.getName(), BASE_CURRENCY.getCurrencyCode())
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
                            .column(ITEMS.CURRENCY_CODE.getName(), BASE_CURRENCY.getCurrencyCode())
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
                            .column(ITEMS.CURRENCY_CODE.getName(), BASE_CURRENCY.getCurrencyCode())
                            .column(ITEMS.MONEY_AMOUNT.getName(), SALARY_AMOUNT)
                            .column(ITEMS.PERIOD.getName(), MONTH.name())
                            .column(ITEMS.ICON.getName(), SALARY_ICON)
                            .column(ITEMS.ITEM_TYPE.getName(), INCOME.name())
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqAccountRepository#findByName(String)} when there are any items.
         */
        @Test
        void shouldFindAccountByNameWithItems() {
            Account account = repository.findByName(ACCOUNT_1_NAME);
            assertThat(account.getName()).isEqualTo(ACCOUNT_1_NAME);
            assertThat(account.getNote()).isNull();
            assertThat(account.getUpdateTime()).isEqualTo(NOW);

            Saving saving = account.getSaving();
            assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, BASE_CURRENCY));
            assertThat(saving.getInterest()).isEqualTo(INTEREST);
            assertThat(saving.isDeposit()).isTrue();
            assertThat(saving.isCapitalization()).isFalse();

            assertThat(account.getItems()).extracting(
                    Item::getId, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
            ).containsExactlyInAnyOrder(
                    tuple(GROCERY_ID, GROCERY, Money.of(GROCERY_AMOUNT, BASE_CURRENCY), DAY, GROCERY_ICON, EXPENSE),
                    tuple(VACATION_ID, VACATION, Money.of(VACATION_AMOUNT, EUR), MONTH, VACATION_ICON, EXPENSE),
                    tuple(SALARY_ID, SALARY, Money.of(SALARY_AMOUNT, BASE_CURRENCY), MONTH, SALARY_ICON, INCOME)
            );
        }

        /**
         * Test for {@link JooqAccountRepository#findByName(String)} when there are no items.
         */
        @Test
        void shouldFindAccountByNameWithoutItems() {
            Account account = repository.findByName(ACCOUNT_2_NAME);
            assertThat(account.getName()).isEqualTo(ACCOUNT_2_NAME);
            assertThat(account.getNote()).isEqualTo(NOTE);
            assertThat(account.getUpdateTime()).isEqualTo(NOW);

            Saving saving = account.getSaving();
            assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(ZERO, EUR));
            assertThat(saving.getInterest()).isEqualTo(ZERO);
            assertThat(saving.isDeposit()).isFalse();
            assertThat(saving.isCapitalization()).isFalse();

            assertThat(account.getItems()).isEmpty();
        }

        /**
         * Test for {@link JooqAccountRepository#findByName(String)} when there is no account with the specified name.
         */
        @Test
        void shouldNotFindAccountByName() {
            assertThat(repository.findByName("noname")).isNull();
        }

        /**
         * Test for {@link AccountRepository#save(Account)} to update an account.
         */
        @Test
        void shouldUpdateAccount() {
//            Income income = stubIncome();
//            Expense expense = stubExpense();
//            Saving saving = stubSaving();
//            transactionTemplate.execute(status -> {
//                Optional<Account> account = repository.findByName(ACCOUNT_1_NAME);
//                assertThat(account).isPresent();
//
//                account.get().update(ImmutableList.of(expense, income), saving, NOTE);
//                repository.save(account.get());
//                return null;
//            });
//
//            Table accounts = new Table(dataSource, Account.TABLE_NAME);
//            Assertions.assertThat(accounts)
//                    .column("name").containsValues(ACCOUNT_1_NAME)
//                    .column("note").containsValues(NOTE)
//                    .column("update_time").hasOnlyNotNullValues();
//
//            Table savings = new Table(dataSource, Saving.TABLE_NAME);
//            Assertions.assertThat(savings)
//                    .column("currency_code").containsValues(BASE_CURRENCY.getCurrencyCode())
//                    .column("amount")
//                    .containsValues(saving.getMoneyAmount().getNumberStripped())
//                    .column("interest").containsValues(saving.getInterest())
//                    .column("deposit").containsValues(true)
//                    .column("capitalization").containsValues(true);
//
//            Table table = new Table(dataSource, Item.TABLE_NAME);
//            Assertions.assertThat(table)
//                    .column("title").containsValues(income.getTitle(), expense.getTitle())
//                    .column("currency_code").containsValues(EUR.getCurrencyCode(), EUR.getCurrencyCode())
//                    .column("amount")
//                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
//                    .column("period").containsValues(MONTH.name(), YEAR.name())
//                    .column("icon").containsValues(income.getIcon(), expense.getIcon());
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
         * Test for {@link AccountRepository#save(Account)} for a new account.
         */
        @Test
        void shouldSaveAccount() {
            Account account = stubAccount();
            transactionTemplate.execute(status -> {
                repository.save(account);
                return null;
            });

            Table accounts = new Table(dataSource, ACCOUNTS.getName());
            Assertions.assertThat(accounts)
                    .column(ACCOUNTS.NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(ACCOUNTS.UPDATE_TIME.getName()).hasOnlyNotNullValues()
                    .column(ACCOUNTS.NOTE.getName()).containsValues(NOTE);

            Table savings = new Table(dataSource, SAVINGS.getName());
            Assertions.assertThat(savings)
                    .column(SAVINGS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME)
                    .column(SAVINGS.CURRENCY_CODE.getName()).containsValues(BASE_CURRENCY.getCurrencyCode())
                    .column(SAVINGS.MONEY_AMOUNT.getName())
                    .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                    .column(SAVINGS.INTEREST.getName()).containsValues(account.getSaving().getInterest())
                    .column(SAVINGS.DEPOSIT.getName()).containsValues(true)
                    .column(SAVINGS.CAPITALIZATION.getName()).containsValues(true);

            Item income = stubIncome();
            Item expense = stubExpense();
            Table table = new Table(dataSource, ITEMS.getName());
            Assertions.assertThat(table)
                    .column(ITEMS.ACCOUNT_NAME.getName()).containsValues(ACCOUNT_1_NAME, ACCOUNT_1_NAME)
                    .column(ITEMS.TITLE.getName()).containsValues(income.getTitle(), expense.getTitle())
                    .column(ITEMS.CURRENCY_CODE.getName()).containsValues(EUR, EUR)
                    .column(ITEMS.MONEY_AMOUNT.getName())
                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                    .column(ITEMS.PERIOD.getName()).containsValues(MONTH.name(), YEAR.name())
                    .column(ITEMS.ICON.getName()).containsValues(income.getIcon(), expense.getIcon())
                    .column(ITEMS.ITEM_TYPE.getName()).containsValues(INCOME.name(), EXPENSE.name());
        }
    }
}
