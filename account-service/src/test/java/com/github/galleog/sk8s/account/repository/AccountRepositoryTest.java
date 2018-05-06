package com.github.galleog.sk8s.account.repository;

import com.github.galleog.sk8s.account.AccountApplication;
import com.github.galleog.sk8s.account.AuditConfig;
import com.github.galleog.sk8s.account.domain.Account;
import com.github.galleog.sk8s.account.domain.AccountBalance;
import com.github.galleog.sk8s.account.domain.Item;
import com.github.galleog.sk8s.account.domain.Saving;
import com.github.galleog.sk8s.account.service.AccountService;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.money.Monetary;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static com.github.galleog.sk8s.account.domain.TimePeriod.*;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {
        AccountApplication.class,
        AuditConfig.class
})
public class AccountRepositoryTest {
    private static final String ACCOUNT_NAME = "test";

    private static final DbSetupTracker dbSetupTracker = new DbSetupTracker();

    @Autowired
    private DataSource dataSource;
    @Autowired
    private AccountRepository repository;

    @Before
    public void setUp() {
        Operation operation = deleteAllFrom(
                "account_items",
                "items",
                "savings",
                "accounts"
        );
        DbSetup dbSetup = new DbSetup(DataSourceDestination.with(dataSource), operation);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void shouldFindAccountByName() {
        Account account = repository.save(stubAccount());

        Optional<Account> found = repository.findByName(ACCOUNT_NAME);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(account.getId());
        assertThat(found.get().getName()).isEqualTo(account.getName());
        assertThat(found.get().getLastModifiedDate()).isEqualTo(account.getLastModifiedDate());
        assertThat(found.get().getNote()).isNull();

        AccountBalance balance = found.get().getBalance();
        assertThat(balance.getIncomes()).hasSize(1);
        assertThat(balance.getExpenses()).hasSize(2);
        assertThat(balance.getSaving()).isNotNull();
    }

    private Account stubAccount() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(1500, AccountService.DEFAULT_CURRENCY))
                .interest(BigDecimal.valueOf(3.32))
                .deposit(true)
                .build();

        Item vacation = Item.builder()
                .title("Vacation")
                .moneyAmount(Money.of(3400, Monetary.getCurrency("EUR")))
                .period(YEAR)
                .icon("tourism")
                .build();
        Item grocery = Item.builder()
                .title("Grocery")
                .moneyAmount(Money.of(10, AccountService.DEFAULT_CURRENCY))
                .period(DAY)
                .icon("meal")
                .build();
        Item salary = Item.builder()
                .title("Salary")
                .moneyAmount(Money.of(9100, AccountService.DEFAULT_CURRENCY))
                .period(MONTH)
                .icon("wallet")
                .build();

        AccountBalance balance = AccountBalance.builder()
                .expense(vacation)
                .expense(grocery)
                .income(salary)
                .saving(saving)
                .build();

        return Account.builder()
                .name("test")
                .balance(balance)
                .build();
    }
}
