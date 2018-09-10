package com.github.galleog.piggymetrics.account.repository;

import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.YEAR;
import static com.github.galleog.piggymetrics.account.service.AccountService.BASE_CURRENCY;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Expense;
import com.github.galleog.piggymetrics.account.domain.Income;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.google.common.collect.ImmutableList;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Tests for {@link AccountRepository}.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
class AccountRepositoryTest {
    private static final int ACCOUNT_ID = 1;
    private static final String ACCOUNT_NAME = "test";
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final double SAVING_AMOUNT = 5900;
    private static final BigDecimal INTEREST = BigDecimal.valueOf(3.32);
    private static final CurrencyUnit EUR = Monetary.getCurrency("EUR");
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

    private Expense stubExpense() {
        return Expense.builder()
                .title("Rent")
                .moneyAmount(Money.of(12000, EUR))
                .period(YEAR)
                .icon("home")
                .build();
    }

    private Income stubIncome() {
        return Income.builder()
                .title("Salary")
                .moneyAmount(Money.of(7500, EUR))
                .period(MONTH)
                .icon("wallet")
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
                .name(ACCOUNT_NAME)
                .item(stubIncome())
                .item(stubExpense())
                .saving(stubSaving())
                .note(NOTE)
                .build();
    }

    @Nested
    class ReadUpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(
                            Item.TABLE_NAME,
                            Saving.TABLE_NAME,
                            Account.TABLE_NAME
                    ),
                    insertInto(Account.TABLE_NAME)
                            .row()
                            .column("id", ACCOUNT_ID)
                            .column("name", ACCOUNT_NAME)
                            .column("last_modified_date", NOW)
                            .column("version", 1)
                            .end()
                            .build(),
                    insertInto(Saving.TABLE_NAME)
                            .row()
                            .column("account_id", ACCOUNT_ID)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SAVING_AMOUNT)
                            .column("interest", INTEREST)
                            .column("deposit", true)
                            .column("capitalization", false)
                            .end()
                            .build(),
                    insertInto(Item.TABLE_NAME)
                            .row()
                            .column("id", GROCERY_ID)
                            .column("account_id", ACCOUNT_ID)
                            .column("title", GROCERY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", GROCERY_AMOUNT)
                            .column("period", DAY)
                            .column("icon", GROCERY_ICON)
                            .column("item_type", "e")
                            .end()
                            .row()
                            .column("id", VACATION_ID)
                            .column("account_id", ACCOUNT_ID)
                            .column("title", VACATION)
                            .column("currency_code", EUR.getCurrencyCode())
                            .column("amount", VACATION_AMOUNT)
                            .column("period", MONTH)
                            .column("icon", VACATION_ICON)
                            .column("item_type", "e")
                            .end()
                            .row()
                            .column("id", SALARY_ID)
                            .column("account_id", ACCOUNT_ID)
                            .column("title", SALARY)
                            .column("currency_code", BASE_CURRENCY.getCurrencyCode())
                            .column("amount", SALARY_AMOUNT)
                            .column("period", MONTH)
                            .column("icon", SALARY_ICON)
                            .column("item_type", "i")
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link AccountRepository#findByName(String)}.
         */
        @Test
        void shouldFindAccountByName() {
            Optional<Account> account = repository.findByName(ACCOUNT_NAME);
            assertThat(account).isPresent();
            assertThat(account.get().getId()).isEqualTo(ACCOUNT_ID);
            assertThat(account.get().getName()).isEqualTo(ACCOUNT_NAME);
            assertThat(account.get().getLastModifiedDate()).isEqualTo(NOW);
            assertThat(account.get().getNote()).isNull();

            Saving saving = account.get().getSaving();
            assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, BASE_CURRENCY));
            assertThat(saving.getInterest()).isEqualTo(INTEREST);
            assertThat(saving.isDeposit()).isTrue();
            assertThat(saving.isCapitalization()).isFalse();

            assertThat(account.get().getIncomes())
                    .extracting(Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon)
                    .containsExactly(tuple(SALARY, Money.of(SALARY_AMOUNT, BASE_CURRENCY), MONTH, SALARY_ICON));
            assertThat(account.get().getExpenses())
                    .extracting(Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon)
                    .containsExactlyInAnyOrder(
                            tuple(GROCERY, Money.of(GROCERY_AMOUNT, BASE_CURRENCY), DAY, GROCERY_ICON),
                            tuple(VACATION, Money.of(VACATION_AMOUNT, EUR), MONTH, VACATION_ICON)
                    );
        }

        /**
         * Test for {@link AccountRepository#save(Object)} to update an account.
         */
        @Test
        void shouldUpdateAccount() {
            Income income = stubIncome();
            Expense expense = stubExpense();
            Saving saving = stubSaving();
            transactionTemplate.execute(status -> {
                Optional<Account> account = repository.findByName(ACCOUNT_NAME);
                assertThat(account).isPresent();

                account.get().update(ImmutableList.of(expense, income), saving, NOTE);
                repository.save(account.get());
                return null;
            });

            Table accounts = new Table(dataSource, Account.TABLE_NAME);
            Assertions.assertThat(accounts)
                    .column("name").containsValues(ACCOUNT_NAME)
                    .column("note").containsValues(NOTE)
                    .column("last_modified_date").hasOnlyNotNullValues();

            Table savings = new Table(dataSource, Saving.TABLE_NAME);
            Assertions.assertThat(savings)
                    .column("currency_code").containsValues(BASE_CURRENCY.getCurrencyCode())
                    .column("amount")
                    .containsValues(saving.getMoneyAmount().getNumberStripped())
                    .column("interest").containsValues(saving.getInterest())
                    .column("deposit").containsValues(true)
                    .column("capitalization").containsValues(true);

            Table table = new Table(dataSource, Item.TABLE_NAME);
            Assertions.assertThat(table)
                    .column("title").containsValues(income.getTitle(), expense.getTitle())
                    .column("currency_code").containsValues(EUR.getCurrencyCode(), EUR.getCurrencyCode())
                    .column("amount")
                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                    .column("period").containsValues(MONTH.name(), YEAR.name())
                    .column("icon").containsValues(income.getIcon(), expense.getIcon());
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(
                            Item.TABLE_NAME,
                            Saving.TABLE_NAME,
                            Account.TABLE_NAME
                    )
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link AccountRepository#save(Object)} for a new account.
         */
        @Test
        void shouldSaveAccount() {
            Account account = stubAccount();
            transactionTemplate.execute(status -> repository.save(account));

            Table accounts = new Table(dataSource, Account.TABLE_NAME);
            Assertions.assertThat(accounts)
                    .column("name").containsValues(ACCOUNT_NAME)
                    .column("last_modified_date").hasOnlyNotNullValues()
                    .column("note").containsValues(NOTE);

            Table savings = new Table(dataSource, Saving.TABLE_NAME);
            Assertions.assertThat(savings)
                    .column("currency_code").containsValues(BASE_CURRENCY.getCurrencyCode())
                    .column("amount")
                    .containsValues(account.getSaving().getMoneyAmount().getNumberStripped())
                    .column("interest").containsValues(account.getSaving().getInterest())
                    .column("deposit").containsValues(true)
                    .column("capitalization").containsValues(true);

            Income income = stubIncome();
            Expense expense = stubExpense();
            Table table = new Table(dataSource, Item.TABLE_NAME);
            Assertions.assertThat(table)
                    .column("title").containsValues(income.getTitle(), expense.getTitle())
                    .column("currency_code").containsValues(EUR.getCurrencyCode(), EUR.getCurrencyCode())
                    .column("amount")
                    .containsValues(income.getMoneyAmount().getNumberStripped(), expense.getMoneyAmount().getNumberStripped())
                    .column("period").containsValues(MONTH.name(), YEAR.name())
                    .column("icon").containsValues(income.getIcon(), expense.getIcon());
        }
    }
}
