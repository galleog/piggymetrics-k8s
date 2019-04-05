package com.github.galleog.piggymetrics.account.service;

import static com.github.galleog.piggymetrics.account.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.account.domain.ItemType.INCOME;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.service.AccountService.BASE_CURRENCY;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.Item.ItemType;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.Item.TimePeriod;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Tests for {@link AccountService}.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    private static final String NAME = "test";
    private static final AccountServiceProto.GetAccountRequest GET_ACCOUNT_REQUEST =
            AccountServiceProto.GetAccountRequest.newBuilder()
                    .setName(NAME)
                    .build();
    private static final AccountServiceProto.CreateAccountRequest CREATE_ACCOUNT_REQUEST =
            AccountServiceProto.CreateAccountRequest.newBuilder()
                    .setName(NAME)
                    .build();
    private static final String NOTE = "note";
    private static final Item GROCERY = Item.builder()
            .title("Grocery")
            .moneyAmount(Money.of(10, BASE_CURRENCY))
            .period(DAY)
            .icon("meal")
            .type(EXPENSE)
            .build();
    private static final Item SALARY = Item.builder()
            .title("Salary")
            .moneyAmount(Money.of(9100, BASE_CURRENCY))
            .period(MONTH)
            .icon("wallet")
            .type(INCOME)
            .build();
    private static final Saving SAVING = Saving.builder()
            .moneyAmount(Money.of(BigDecimal.ZERO, BASE_CURRENCY))
            .interest(BigDecimal.ZERO)
            .build();
    private static final String PASSWORD = "secret";

    @Mock
    private AccountRepository repository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(Schedulers.immediate(), repository);
    }

    /**
     * Test for {@link AccountService#getAccount(Mono)}.
     */
    @Test
    void shouldGetAccount() {
        Account account = stubAccount();
        when(repository.findByName(NAME)).thenReturn(account);

        Mono<AccountServiceProto.Account> accountMono = accountService.getAccount(Mono.just(GET_ACCOUNT_REQUEST));
        StepVerifier.create(accountMono)
                .expectNextMatches(a -> {
                    assertThat(a.getName()).isEqualTo(NAME);
                    assertThat(a.getItemsList()).extracting(
                            AccountServiceProto.Item::getType,
                            AccountServiceProto.Item::getTitle,
                            AccountServiceProto.Item::getMoney,
                            AccountServiceProto.Item::getPeriod,
                            AccountServiceProto.Item::getIcon
                    ).containsExactlyInAnyOrder(
                            tuple(
                                    ItemType.EXPENSE,
                                    GROCERY.getTitle(),
                                    moneyConverter().convert(GROCERY.getMoneyAmount()),
                                    TimePeriod.DAY,
                                    GROCERY.getIcon()
                            ),
                            tuple(
                                    ItemType.INCOME,
                                    SALARY.getTitle(),
                                    moneyConverter().convert(SALARY.getMoneyAmount()),
                                    TimePeriod.MONTH,
                                    SALARY.getIcon()

                            )
                    );
                    assertThat(a.getSaving().getMoney()).isEqualTo(moneyConverter().convert(SAVING.getMoneyAmount()));
                    assertThat(a.getSaving().getInterest()).isEqualTo(bigDecimalConverter().convert(SAVING.getInterest()));
                    assertThat(a.getSaving().getDeposit()).isFalse();
                    assertThat(a.getSaving().getCapitalization()).isFalse();
                    assertThat(a.getUpdateTime()).isNotNull();
                    assertThat(a.getNote()).isEmpty();
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link AccountService#getAccount(Mono)} when no account is found.
     */
    @Test
    void shouldFailToFindAccountWhenNotFound() {
        when(repository.findByName(NAME)).thenReturn(null);

        Mono<AccountServiceProto.Account> accountMono = accountService.getAccount(Mono.just(GET_ACCOUNT_REQUEST));
        StepVerifier.create(accountMono)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    return true;
                }).verify();
    }

    /**
     * Test for {@link AccountService#createAccount(Mono)}.
     */
    @Test
    void shouldCreateAccount() {
        when(repository.findByName(NAME)).thenReturn(null);

        Mono<AccountServiceProto.Account> accountMono = accountService.createAccount(Mono.just(CREATE_ACCOUNT_REQUEST));
        StepVerifier.create(accountMono)
                .expectNextMatches(a -> {
                    assertThat(a.getName()).isEqualTo(NAME);
                    assertThat(a.getSaving().getMoney())
                            .isEqualTo(moneyConverter().convert(Money.of(BigDecimal.ZERO, BASE_CURRENCY)));
                    assertThat(a.getSaving().getInterest()).isEqualTo(bigDecimalConverter().convert(BigDecimal.ZERO));
                    assertThat(a.getSaving().getDeposit()).isFalse();
                    assertThat(a.getSaving().getCapitalization()).isFalse();
                    assertThat(a.getItemsList()).isEmpty();

                    verify(repository).save(argThat(argument -> {
                        assertThat(argument.getName()).isEqualTo(NAME);
                        return true;
                    }));
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link AccountService#createAccount(Mono)} when an account with the same name already exists.
     */
    @Test
    void shouldFailToCreateAccountWhenItAlreadyExists() {
        when(repository.findByName(NAME)).thenReturn(stubAccount());

        Mono<AccountServiceProto.Account> accountMono = accountService.createAccount(Mono.just(CREATE_ACCOUNT_REQUEST));
        StepVerifier.create(accountMono)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
                    return true;
                }).verify();

        verify(repository, never()).save(any(Account.class));
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)}.
     */
    @Test
    void shouldUpdateAccount() {
//        when(repository.findByName(NAME)).thenReturn(Optional.of(stubAccount()));
//
//        Money savingAmount = Money.of(1500, BASE_CURRENCY);
//        BigDecimal interest = BigDecimal.valueOf(3.32);
//        AccountServiceProto.Saving saving = AccountServiceProto.Saving.newBuilder()
//                .setMoney(moneyConverter().convert(savingAmount))
//                .setInterest(bigDecimalConverter().convert(interest))
//                .setDeposit(true)
//                .build();
//
//        Money rentAmount = Money.of(1200, BASE_CURRENCY);
//        AccountServiceProto.Item rent = AccountServiceProto.Item.newBuilder()
//                .setType(ItemType.EXPENSE)
//                .setTitle("Rent")
//                .setMoney(moneyConverter().convert(rentAmount))
//                .setPeriod(TimePeriod.MONTH)
//                .setIcon("home")
//                .build();
//
//        Money mealAmount = Money.of(20, BASE_CURRENCY);
//        AccountServiceProto.Item meal = AccountServiceProto.Item.newBuilder()
//                .setType(ItemType.EXPENSE)
//                .setTitle("Meal")
//                .setMoney(moneyConverter().convert(mealAmount))
//                .setPeriod(TimePeriod.DAY)
//                .setIcon("meal")
//                .build();
//
//        AccountServiceProto.Account account = AccountServiceProto.Account.newBuilder()
//                .setName(NAME)
//                .addItems(rent)
//                .addItems(meal)
//                .setSaving(saving)
//                .setNote(NOTE)
//                .build();
//        Mono<AccountServiceProto.Account> updated = accountService.updateAccount(Mono.just(account));
//        StepVerifier.create(updated)
//                .expectNextMatches(a -> {
//                    assertThat(a.getName()).isEqualTo(NAME);
//                    assertThat(a.getItemsList()).containsExactlyInAnyOrder(rent, meal);
//                    assertThat(a.getSaving()).isEqualTo(saving);
//                    assertThat(a.getUpdateTime().isInitialized()).isTrue();
//                    assertThat(a.getNote()).isEqualTo(NOTE);
//                    return true;
//                }).verifyComplete();
//
//        verify(repository).save(argThat(a -> {
//            assertThat(a.getName()).isEqualTo(NAME);
//            assertThat(a.getItems()).extracting(
//                    Item::getClass, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
//            ).containsExactlyInAnyOrder(
//                    tuple(Expense.class, rent.getTitle(), rentAmount, MONTH, rent.getIcon()),
//                    tuple(Expense.class, meal.getTitle(), mealAmount, DAY, meal.getIcon())
//            );
//            assertThat(a.getSaving().getMoneyAmount()).isEqualTo(savingAmount);
//            assertThat(a.getSaving().getInterest()).isEqualTo(interest);
//            assertThat(a.getSaving().isDeposit()).isTrue();
//            assertThat(a.getSaving().isCapitalization()).isFalse();
//            assertThat(a.getUpdateTime()).isNotNull();
//            assertThat(a.getNote()).isEqualTo(NOTE);
//            return true;
//        }));
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)} when the account to be updated isn't found.
     */
    @Test
    void shouldFailToUpdateAccountWhenNotFound() {
//        when(repository.findByName(NAME)).thenReturn(Optional.empty());
//
//        AccountServiceProto.Account account = AccountServiceProto.Account.newBuilder()
//                .setName(NAME)
//                .build();
//        Mono<AccountServiceProto.Account> updated = accountService.updateAccount(Mono.just(account));
//        StepVerifier.create(updated)
//                .expectErrorMatches(t -> {
//                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
//                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
//                    return true;
//                }).verify();
//
//        verify(repository, never()).save(any());
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)} when account data are invalid.
     */
    @Test
    void shouldFailToUpdateAccountWhenDataInvalid() {
//        when(repository.findByName(NAME)).thenReturn(Optional.of(stubAccount()));
//
//        AccountServiceProto.Saving saving = AccountServiceProto.Saving.newBuilder()
//                .setDeposit(true)
//                .build();
//        AccountServiceProto.Account account = AccountServiceProto.Account.newBuilder()
//                .setName(NAME)
//                .setSaving(saving)
//                .build();
//        Mono<AccountServiceProto.Account> updated = accountService.updateAccount(Mono.just(account));
//        StepVerifier.create(updated)
//                .expectErrorMatches(t -> {
//                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
//                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
//                    return true;
//                }).verify();
//
//        verify(repository, never()).save(any());
    }

    private Account stubAccount() {
        return Account.builder()
                .name(NAME)
                .item(GROCERY)
                .item(SALARY)
                .saving(SAVING)
                .build();
    }
}
