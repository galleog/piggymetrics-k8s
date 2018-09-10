package com.github.galleog.piggymetrics.account.service;

import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.service.AccountService.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.client.AuthServiceClient;
import com.github.galleog.piggymetrics.account.client.StatisticsServiceClient;
import com.github.galleog.piggymetrics.account.acl.AccountDto;
import com.github.galleog.piggymetrics.account.acl.User;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Expense;
import com.github.galleog.piggymetrics.account.domain.Income;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Tests for {@link AccountService}.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    private static final String NAME = "test";
    private static final String NOTE = "note";

    @Mock
    private StatisticsServiceClient statisticsClient;
    @Mock
    private AuthServiceClient authClient;
    @Mock
    private AccountRepository repository;
    @InjectMocks
    private AccountService accountService;
    @Captor
    private ArgumentCaptor<AccountDto> accountCaptor;

    /**
     * Test for {@link AccountService#findByName(String)}.
     */
    @Test
    void shouldFindByName() {
        Account account = stubAccount();
        when(repository.findByName(NAME)).thenReturn(Optional.of(account));
        Optional<Account> found = accountService.findByName(NAME);
        assertThat(found).containsSame(account);
    }

    /**
     * Test for an invalid name in {@link AccountService#findByName(String)}.
     */
    @Test
    void shouldFailWhenNameIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                accountService.findByName(StringUtils.EMPTY)
        );
    }

    /**
     * Test for {@link AccountService#create(User)}.
     */
    @Test
    void shouldCreateAccountWithGivenUser() {
        User user = stubUser();
        when(repository.findByName(NAME)).thenReturn(Optional.empty());
        when(repository.save(any(Account.class))).thenAnswer((Answer<Account>) invocation ->
                invocation.getArgument(0)
        );

        Account account = accountService.create(user);
        assertThat(account.getName()).isEqualTo(NAME);
        assertThat(account.getSaving().getMoneyAmount()).isEqualTo(Money.of(0, BASE_CURRENCY));
        assertThat(account.getSaving().getInterest()).isEqualTo(BigDecimal.ZERO);
        assertThat(account.getSaving().isDeposit()).isFalse();
        assertThat(account.getSaving().isCapitalization()).isFalse();
        assertThat(account.getIncomes()).isEmpty();
        assertThat(account.getExpenses()).isEmpty();

        verify(authClient).createUser(user);
        verify(repository).save(account);
    }

    /**
     * Test for {@link AccountService#create(User)} if an account with the same name already exists.
     */
    @Test
    void shouldFailIfAccountAlreadyExists() {
        when(repository.findByName(NAME)).thenReturn(Optional.of(stubAccount()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> accountService.create(stubUser()));
    }

    /**
     * Test for {@link AccountService#update(String, java.util.Collection, Saving, String)}.
     */
    @Test
    void shouldUpdateAccountByGivenOne() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(1500, BASE_CURRENCY))
                .interest(BigDecimal.valueOf(3.32))
                .deposit(true)
                .build();

        Expense grocery = Expense.builder()
                .title("Grocery")
                .moneyAmount(Money.of(10, BASE_CURRENCY))
                .period(DAY)
                .icon("meal")
                .build();
        Income salary = Income.builder()
                .title("Salary")
                .moneyAmount(Money.of(9100, BASE_CURRENCY))
                .period(MONTH)
                .icon("wallet")
                .build();

        Account account = stubAccount();
        when(repository.findByName(NAME)).thenReturn(Optional.of(account));
        doNothing().when(statisticsClient).updateStatistics(eq(NAME), accountCaptor.capture());

        accountService.update(NAME, ImmutableList.of(grocery, salary), saving, NOTE);
        assertThat(account.getSaving().getMoneyAmount()).isEqualTo(saving.getMoneyAmount());
        assertThat(account.getSaving().getInterest()).isEqualTo(saving.getInterest());
        assertThat(account.getSaving().isDeposit()).isTrue();
        assertThat(account.getSaving().isCapitalization()).isFalse();
        assertThat(account.getIncomes()).extracting(
                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
        ).containsExactly(tuple(salary.getTitle(), salary.getMoneyAmount(), MONTH, salary.getIcon()));
        assertThat(account.getExpenses()).extracting(
                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
        ).containsExactly(tuple(grocery.getTitle(), grocery.getMoneyAmount(), DAY, grocery.getIcon()));
        assertThat(account.getNote()).isEqualTo(NOTE);

        AccountDto updated = accountCaptor.getValue();
        assertThat(updated.getName()).isNull();
        assertThat(updated.getExpenses()).containsExactlyInAnyOrderElementsOf(account.getExpenses());
        assertThat(updated.getIncomes()).containsExactlyInAnyOrderElementsOf(account.getIncomes());
        assertThat(updated.getSaving()).isSameAs(account.getSaving());
        assertThat(updated.getNote()).isNull();
        assertThat(updated.getLastModifiedDate()).isNull();

        verify(repository).save(account);
    }

    private Account stubAccount() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(0, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .build();
        return Account.builder()
                .name(NAME)
                .saving(saving)
                .build();
    }

    private User stubUser() {
        return User.builder()
                .username(NAME)
                .password("secret")
                .build();
    }
}
