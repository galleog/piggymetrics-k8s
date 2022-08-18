package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.config.RouterConfig.DEMO_ACCOUNT;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.timestampConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.ItemType;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.TimePeriod;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc.AccountServiceImplBase;
import com.github.galleog.piggymetrics.apigateway.model.account.Account;
import com.github.galleog.piggymetrics.apigateway.model.account.Item;
import com.github.galleog.piggymetrics.apigateway.model.account.Saving;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tests for routing account requests.
 */
class AccountRequestRouterTest extends BaseRouterTest {
    private static final String ACCOUNT_NAME = "test";
    private static final String CURRENCY = "USD";
    private static final String ACCOUNT_NOTE = "note";
    private static final Money SAVING_AMOUNT = Money.of(BigDecimal.valueOf(150000, 2), CURRENCY);
    private static final BigDecimal SAVING_INTEREST = BigDecimal.valueOf(3.32);
    private static final String GROCERY = "Grocery";
    private static final Money GROCERY_AMOUNT = Money.of(BigDecimal.valueOf(1000, 2), CURRENCY);
    private static final String GROCERY_ICON = "meal";
    private static final String SALARY = "Salary";
    private static final Money SALARY_AMOUNT = Money.of(BigDecimal.valueOf(910000, 2), CURRENCY);
    private static final String SALARY_ICON = "wallet";
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.now();

    @Captor
    private ArgumentCaptor<Mono<AccountServiceProto.GetAccountRequest>> getAccountRequestCaptor;
    @Captor
    private ArgumentCaptor<Mono<AccountServiceProto.Account>> accountCaptor;

    private AccountServiceImplBase accountService;

    @BeforeEach
    void setUp() throws Exception {
        accountService = spyGrpcService(AccountServiceImplBase.class, AccountHandler.ACCOUNT_SERVICE);
    }

    /**
     * Test for GET /accounts/demo.
     */
    @Test
    void shouldGetDemoAccount() {
        doReturn(Mono.just(stubAccountProto(DEMO_ACCOUNT))).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.get()
                .uri("/accounts/demo")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Account.class)
                .value(account -> {
                    assertThat(account.getName()).isEqualTo(DEMO_ACCOUNT);
                    assertThat(account.getItems()).extracting(
                            Item::getType, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
                    ).containsExactlyInAnyOrder(
                            tuple(ItemType.EXPENSE, GROCERY, GROCERY_AMOUNT, TimePeriod.DAY, GROCERY_ICON),
                            tuple(ItemType.INCOME, SALARY, SALARY_AMOUNT, TimePeriod.MONTH, SALARY_ICON)
                    );
                    assertThat(account.getSaving()).extracting(
                            Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
                    ).containsExactly(
                            SAVING_AMOUNT, SAVING_INTEREST, true, false
                    );
                    assertThat(account.getNote()).isEqualTo(ACCOUNT_NOTE);
                    assertThat(account.getUpdateTime()).isEqualTo(UPDATE_TIME);
                });

        getAccountRequestCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(value -> DEMO_ACCOUNT.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for GET /accounts/current.
     */
    @Test
    void shouldGetAccountOfCurrentUser() {
        doReturn(Mono.just(stubAccountProto(ACCOUNT_NAME))).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isOk();

        getAccountRequestCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(value -> ACCOUNT_NAME.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for GET /accounts/current when no current account is found.
     */
    @Test
    void shouldFailToGetCurrentAccountIfItDoesNotExist() {
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isNotFound();

        getAccountRequestCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(value -> ACCOUNT_NAME.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for GET /accounts/current without authentication.
     */
    @Test
    void shouldFailToGetCurrentAccountWithoutAuthentication() {
        webClient.get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test for PUT /accounts/current.
     */
    @Test
    void shouldUpdateCurrentAccount() {
        doReturn(Mono.just(stubAccountProto(ACCOUNT_NAME))).when(accountService).updateAccount(accountCaptor.capture());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .mutateWith(csrf())
                .put()
                .uri("/accounts/current")
                .bodyValue(stubAccount())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Account.class)
                .value(account -> {
                    assertThat(account.getName()).isEqualTo(ACCOUNT_NAME);
                    assertThat(account.getItems()).extracting(
                            Item::getType, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
                    ).containsExactlyInAnyOrder(
                            tuple(ItemType.EXPENSE, GROCERY, GROCERY_AMOUNT, TimePeriod.DAY, GROCERY_ICON),
                            tuple(ItemType.INCOME, SALARY, SALARY_AMOUNT, TimePeriod.MONTH, SALARY_ICON)
                    );
                    assertThat(account.getSaving()).extracting(
                            Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
                    ).containsExactly(
                            SAVING_AMOUNT, SAVING_INTEREST, true, false
                    );
                    assertThat(account.getNote()).isEqualTo(ACCOUNT_NOTE);
                    assertThat(account.getUpdateTime()).isEqualTo(UPDATE_TIME);
                });

        accountCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(account -> {
                    assertThat(account.getName()).isEqualTo(ACCOUNT_NAME);
                    assertThat(account.getItemsList()).extracting(
                            AccountServiceProto.Item::getType,
                            AccountServiceProto.Item::getTitle,
                            AccountServiceProto.Item::getMoney,
                            AccountServiceProto.Item::getPeriod,
                            AccountServiceProto.Item::getIcon
                    ).containsExactlyInAnyOrder(
                            tuple(
                                    ItemType.EXPENSE,
                                    GROCERY,
                                    moneyConverter().convert(GROCERY_AMOUNT),
                                    TimePeriod.DAY,
                                    GROCERY_ICON
                            ),
                            tuple(
                                    ItemType.INCOME,
                                    SALARY,
                                    moneyConverter().convert(SALARY_AMOUNT),
                                    TimePeriod.MONTH,
                                    SALARY_ICON
                            )
                    );
                    assertThat(account.getSaving()).extracting(
                            AccountServiceProto.Saving::getMoney,
                            AccountServiceProto.Saving::getInterest,
                            AccountServiceProto.Saving::getDeposit,
                            AccountServiceProto.Saving::getCapitalization
                    ).containsExactly(
                            moneyConverter().convert(SAVING_AMOUNT),
                            bigDecimalConverter().convert(SAVING_INTEREST),
                            true,
                            false
                    );
                    assertThat(account.getNote()).isEqualTo(ACCOUNT_NOTE);
                    assertThat(account.hasUpdateTime()).isFalse();
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for PUT /accounts/current if there exists no account with the principal's name.
     */
    @Test
    void shouldFailToUpdateCurrentAccountIfItDoesNotExist() {
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).updateAccount(accountCaptor.capture());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .mutateWith(csrf())
                .put()
                .uri("/accounts/current")
                .bodyValue(stubAccount())
                .exchange()
                .expectStatus().isNotFound();

        accountCaptor.getValue()
                .as(StepVerifier::create)
                .expectNextMatches(account -> ACCOUNT_NAME.equals(account.getName()))
                .verifyComplete();
    }

    /**
     * Test for PUT /accounts/current without authentication.
     */
    @Test
    void shouldFailToUpdateCurrentAccountWithoutAuthentication() {
        webClient.mutateWith(csrf())
                .put()
                .uri("/accounts/current")
                .bodyValue(stubAccount())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private AccountServiceProto.Account stubAccountProto(String name) {
        return AccountServiceProto.Account.newBuilder()
                .setName(name)
                .addItems(stubExpenseProto())
                .addItems(stubIncomeProto())
                .setSaving(stubSavingProto())
                .setUpdateTime(timestampConverter().convert(UPDATE_TIME))
                .setNote(ACCOUNT_NOTE)
                .build();
    }

    private AccountServiceProto.Saving stubSavingProto() {
        return AccountServiceProto.Saving.newBuilder()
                .setMoney(moneyConverter().convert(SAVING_AMOUNT))
                .setInterest(bigDecimalConverter().convert(SAVING_INTEREST))
                .setDeposit(true)
                .build();
    }

    private AccountServiceProto.Item stubExpenseProto() {
        return AccountServiceProto.Item.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle(GROCERY)
                .setMoney(moneyConverter().convert(GROCERY_AMOUNT))
                .setPeriod(TimePeriod.DAY)
                .setIcon(GROCERY_ICON)
                .build();
    }

    private AccountServiceProto.Item stubIncomeProto() {
        return AccountServiceProto.Item.newBuilder()
                .setType(ItemType.INCOME)
                .setTitle(SALARY)
                .setMoney(moneyConverter().convert(SALARY_AMOUNT))
                .setPeriod(TimePeriod.MONTH)
                .setIcon(SALARY_ICON)
                .build();
    }

    private Account stubAccount() {
        Item grocery = Item.builder()
                .type(ItemType.EXPENSE)
                .title(GROCERY)
                .moneyAmount(GROCERY_AMOUNT)
                .period(TimePeriod.DAY)
                .icon(GROCERY_ICON)
                .build();
        Item salary = Item.builder()
                .type(ItemType.INCOME)
                .title(SALARY)
                .moneyAmount(SALARY_AMOUNT)
                .period(TimePeriod.MONTH)
                .icon(SALARY_ICON)
                .build();
        Saving saving = Saving.builder()
                .moneyAmount(SAVING_AMOUNT)
                .interest(SAVING_INTEREST)
                .deposit(true)
                .build();
        return Account.builder()
                .item(grocery)
                .item(salary)
                .saving(saving)
                .note(ACCOUNT_NOTE)
                .build();
    }
}
