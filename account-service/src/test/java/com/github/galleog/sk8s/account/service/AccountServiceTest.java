package com.github.galleog.sk8s.account.service;

import com.github.galleog.sk8s.account.client.AuthServiceClient;
import com.github.galleog.sk8s.account.client.StatisticsServiceClient;
import com.github.galleog.sk8s.account.domain.*;
import com.github.galleog.sk8s.account.repository.AccountRepository;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.Optional;

import static com.github.galleog.sk8s.account.domain.TimePeriod.DAY;
import static com.github.galleog.sk8s.account.domain.TimePeriod.MONTH;
import static com.github.galleog.sk8s.account.service.AccountService.DEFAULT_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AccountService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {
    private static final String NAME = "test";

    @Mock
    private StatisticsServiceClient statisticsClient;
    @Mock
    private AuthServiceClient authClient;
    @Mock
    private AccountRepository repository;
    @InjectMocks
    private AccountService accountService;

    /**
     * Test for {@link AccountService#findByName(String)}.
     */
    @Test
    public void shouldFindByName() {
        Account account = stubAccount();
        when(repository.findByName(NAME)).thenReturn(Optional.of(account));
        Optional<Account> found = accountService.findByName(NAME);
        assertThat(found).containsSame(account);
    }

    /**
     * Test for an invalid name in {@link AccountService#findByName(String)}.
     */
    @Test
    public void shouldFailWhenNameIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                accountService.findByName(StringUtils.EMPTY)
        );
    }

    /**
     * Test for {@link AccountService#create(User)}.
     */
    @Test
    public void shouldCreateAccountWithGivenUser() {
        User user = stubUser();
        when(repository.findByName(NAME)).thenReturn(Optional.empty());
        when(repository.save(any(Account.class))).thenAnswer((Answer<Account>) invocation ->
                invocation.getArgument(0)
        );

        Account account = accountService.create(user);
        assertThat(account.getName()).isEqualTo(NAME);
        assertThat(account.getBalance().getSaving().getMoneyAmount()).isEqualTo(Money.of(0, DEFAULT_CURRENCY));
        assertThat(account.getBalance().getSaving().getInterest()).isEqualTo(BigDecimal.ZERO);
        assertThat(account.getBalance().getSaving().isDeposit()).isFalse();
        assertThat(account.getBalance().getSaving().isCapitalization()).isFalse();
        assertThat(account.getBalance().getIncomes()).isEmpty();
        assertThat(account.getBalance().getExpenses()).isEmpty();

        verify(authClient).createUser(user);
        verify(repository).save(account);
    }

    /**
     * Test for {@link AccountService#create(User)} if an account with the same name already exists.
     */
    @Test
    public void shouldFailIfAccountAlreadyExists() {
        when(repository.findByName(NAME)).thenReturn(Optional.of(stubAccount()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> accountService.create(stubUser()));
    }

    /**
     * Test for {@link AccountService#update(String, Account)}.
     */
    @Test
    public void shouldUpdateAccountByGivenOne() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(1500, AccountService.DEFAULT_CURRENCY))
                .interest(BigDecimal.valueOf(3.32))
                .deposit(true)
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
                .expense(grocery)
                .income(salary)
                .saving(saving)
                .build();

        Account update = Account.builder()
                .name(NAME)
                .balance(balance)
                .note("note")
                .build();

        Account account = stubAccount();
        when(repository.findByName(NAME)).thenReturn(Optional.of(account));

        accountService.update(NAME, update);
        assertThat(account.getBalance()).isSameAs(balance);
        assertThat(account.getNote()).isEqualTo(update.getNote());

        verify(repository).save(account);
        verify(statisticsClient).updateStatistics(NAME, account.getBalance());
    }

    private Account stubAccount() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(0, DEFAULT_CURRENCY))
                .interest(BigDecimal.ZERO)
                .build();
        AccountBalance balance = AccountBalance.builder()
                .saving(saving)
                .build();
        return Account.builder()
                .name(NAME)
                .balance(balance)
                .build();
    }

    private User stubUser() {
        return User.builder()
                .username(NAME)
                .password("secret")
                .build();
    }
}
