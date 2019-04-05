package com.github.galleog.piggymetrics.gateway.handler;

import static com.github.galleog.piggymetrics.gateway.handler.AccountHandler.DEMO_ACCOUNT;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.Item.ItemType;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.Item.TimePeriod;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.gateway.dto.Account;
import com.github.galleog.piggymetrics.gateway.dto.Item;
import com.github.galleog.piggymetrics.gateway.dto.Saving;
import com.github.galleog.piggymetrics.gateway.dto.User;
import com.github.galleog.protobuf.java.type.MoneyProto;
import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.security.Principal;

/**
 * Tests for routing account requests.
 */
@RunWith(SpringRunner.class)
public class AccountRequestRouterTest extends BaseRouterTest {
    private static final String CURRENCY = "USD";
    private static final String ACCOUNT_NOTE = "note";
    private static final double SAVING_AMOUNT = 1500;
    private static final BigDecimal SAVING_INTEREST = BigDecimal.valueOf(3.32);
    private static final String GROCERY = "Grocery";
    private static final double GROCERY_AMOUNT = 10;
    private static final String GROCERY_ICON = "meal";
    private static final String SALARY = "Salary";
    private static final double SALARY_AMOUNT = 9100;
    private static final String SALARY_ICON = "wallet";

    @Captor
    private ArgumentCaptor<Mono<AccountServiceProto.GetAccountRequest>> getAccountRequestCaptor;
    @Captor
    private ArgumentCaptor<Mono<UserServiceProto.User>> userCaptor;

    private ReactorAccountServiceGrpc.AccountServiceImplBase accountService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        accountService = spy(new ReactorAccountServiceGrpc.AccountServiceImplBase() {
        });

        grpcCleanup.register(InProcessServerBuilder.forName(AccountHandler.ACCOUNT_SERVICE)
                .directExecutor()
                .addService(accountService)
                .build()
                .start());
    }

    /**
     * Test for GET /accounts/demo.
     */
    @Test
    public void shouldGetDemoAccount() {
        doReturn(Mono.just(stubAccountProto(DEMO_ACCOUNT))).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.get()
                .uri("/accounts/demo")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Account.class)
                .value(account -> {
                    assertThat(account.getName()).isEqualTo(DEMO_ACCOUNT);
                    assertThat(account.getItems()).extracting(
                            Item::getType, Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
                    ).containsExactlyInAnyOrder(
                            tuple(ItemType.EXPENSE, GROCERY, Money.of(GROCERY_AMOUNT, CURRENCY), TimePeriod.DAY, GROCERY_ICON),
                            tuple(ItemType.INCOME, SALARY, Money.of(SALARY_AMOUNT, CURRENCY), TimePeriod.MONTH, SALARY_ICON)
                    );
                    assertThat(account.getSaving()).extracting(
                            Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
                    ).containsExactly(
                            Money.of(SAVING_AMOUNT, CURRENCY), SAVING_INTEREST, true, false
                    );
                    assertThat(account.getNote()).isEqualTo(ACCOUNT_NOTE);
                    assertThat(account.getUpdateTime()).isNotNull();
                });

        StepVerifier.create(getAccountRequestCaptor.getValue())
                .expectNextMatches(value -> DEMO_ACCOUNT.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for GET /accounts/current.
     */
    @Test
    public void shouldGetAccountOfCurrentUser() {
        doReturn(Mono.just(stubAccountProto(ACCOUNT_NAME))).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isOk();

        StepVerifier.create(getAccountRequestCaptor.getValue())
                .expectNextMatches(value -> ACCOUNT_NAME.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for GET /accounts/current when no current account is found.
     */
    @Test
    public void shouldFailIfAccountDoesNotExist() {
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).getAccount(getAccountRequestCaptor.capture());

        webClient.get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isNotFound();

        StepVerifier.create(getAccountRequestCaptor.getValue())
                .expectNextMatches(value -> ACCOUNT_NAME.equals(value.getName()))
                .verifyComplete();
    }

    /**
     * Test for POST /accounts.
     */
    @Test
    public void shouldCreateNewAccount() {
        doReturn(Mono.just(stubAccountProto(ACCOUNT_NAME))).when(accountService).createAccount(userCaptor.capture());

        User user = User.builder()
                .username(ACCOUNT_NAME)
                .password(PASSWORD)
                .build();
        webClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(user)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Account.class)
                .value(account -> {
                    assertThat(account.getName()).isEqualTo(ACCOUNT_NAME);
                });

        StepVerifier.create(userCaptor.getValue())
                .expectNextMatches(u -> {
                    assertThat(u.getUserName()).isEqualTo(ACCOUNT_NAME);
                    assertThat(u.getPassword()).isEqualTo(PASSWORD);
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link AccountHandler#updateCurrentAccount(Principal, AccountDto)}.
     */
//    @Test
//    @SuppressWarnings("unchecked")
//    void shouldUpdateCurrentAccount() throws Exception {
//        AccountDto account = stubAccountDto();
//        ArgumentCaptor<List<Item>> itemsCaptor = ArgumentCaptor.forClass(List.class);
//        ArgumentCaptor<Saving> savingCaptor = ArgumentCaptor.forClass(Saving.class);
//        when(accountService.update(eq(ACCOUNT_NAME), itemsCaptor.capture(), savingCaptor.capture(), eq(ACCOUNT_NOTE)))
//                .thenReturn(Optional.of(stubAccount()));
//
//        byte[] json = objectMapper.writeValueAsBytes(account);
//        mockMvc.perform(put(ACCOUNTS_BASE_URL + "/current").contentType(MediaType.APPLICATION_JSON).content(json))
//                .andExpect(status().isNoContent());
//
//        Assertions.assertThat(itemsCaptor.getValue()).extracting(
//                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
//        ).containsExactlyInAnyOrder(
//                tuple(GROCERY, Money.of(GROCERY_AMOUNT, AccountService.BASE_CURRENCY), TimePeriod.DAY, GROCERY_ICON),
//                tuple(SALARY, Money.of(SALARY_AMOUNT, AccountService.BASE_CURRENCY), TimePeriod.MONTH, SALARY_ICON)
//        );
//
//        Saving saving = savingCaptor.getValue();
//        Assertions.assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, AccountService.BASE_CURRENCY));
//        Assertions.assertThat(saving.getInterest()).isEqualTo(BigDecimal.valueOf(SAVING_INTEREST));
//        Assertions.assertThat(saving.isDeposit()).isTrue();
//        Assertions.assertThat(saving.isCapitalization()).isFalse();
//    }

    /**
     * Test for {@link AccountHandler#updateCurrentAccount(ServerRequest)}
     * if there exists no account with the principal's name.
     */
//    @Test
//    void shouldFailIfNoAccountWithPrincipalNameExistsInUpdateCurrentAccount() throws Exception {
//        when(accountService.update(anyString(), anyList(), any(Saving.class), anyString())).thenReturn(Optional.empty());
//
//        byte[] json = objectMapper.writeValueAsBytes(stubAccountDto());
//        mockMvc.perform(put(ACCOUNTS_BASE_URL + "/current").contentType(MediaType.APPLICATION_JSON).content(json))
//                .andExpect(status().isNotFound());
//    }
    private AccountServiceProto.Account stubAccountProto(String name) {
        return AccountServiceProto.Account.newBuilder()
                .setName(name)
                .addItems(stubExpenseProto())
                .addItems(stubIncomeProto())
                .setSaving(stubSavingProto())
                .setUpdateTime(Timestamps.fromMillis(System.currentTimeMillis()))
                .setNote(ACCOUNT_NOTE)
                .build();
    }

    private AccountServiceProto.Saving stubSavingProto() {
        return AccountServiceProto.Saving.newBuilder()
                .setMoney(toMoneyProto(SAVING_AMOUNT))
                .setInterest(bigDecimalConverter().convert(SAVING_INTEREST))
                .setDeposit(true)
                .build();
    }

    private AccountServiceProto.Item stubExpenseProto() {
        return AccountServiceProto.Item.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle(GROCERY)
                .setMoney(toMoneyProto(GROCERY_AMOUNT))
                .setPeriod(TimePeriod.DAY)
                .setIcon(GROCERY_ICON)
                .build();
    }

    private AccountServiceProto.Item stubIncomeProto() {
        return AccountServiceProto.Item.newBuilder()
                .setType(ItemType.INCOME)
                .setTitle(SALARY)
                .setMoney(toMoneyProto(SALARY_AMOUNT))
                .setPeriod(TimePeriod.MONTH)
                .setIcon(SALARY_ICON)
                .build();
    }

    private MoneyProto.Money toMoneyProto(double amount) {
        return MoneyProto.Money.newBuilder()
                .setAmount(bigDecimalConverter().convert(BigDecimal.valueOf(amount)))
                .setCurrencyCode(CURRENCY)
                .build();
    }

//    private byte[] makeUserJson() throws JsonProcessingException {
//        User user = User.builder()
//                .username(ACCOUNT_NAME)
//                .password(PASSWORD)
//                .build();
//        return objectMapper.writeValueAsBytes(user);
//    }
}
