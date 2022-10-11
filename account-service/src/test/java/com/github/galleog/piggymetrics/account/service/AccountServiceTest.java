package com.github.galleog.piggymetrics.account.service;

import static com.github.galleog.piggymetrics.account.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.account.domain.ItemType.INCOME;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.GetAccountRequest;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.ItemType;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.TimePeriod;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Tests for {@link AccountService}.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    private static final String USD = "USD";
    private static final String NAME = "test";
    private static final String TOPIC = "test-topic";
    private static final Mono<GetAccountRequest> GET_ACCOUNT_REQUEST = Mono.just(
            GetAccountRequest.newBuilder()
                    .setName(NAME)
                    .build()
    );
    private static final String NOTE = "note";
    private static final Item GROCERY = Item.builder()
            .title("Grocery")
            .moneyAmount(Money.of(10, USD))
            .period(DAY)
            .icon("meal")
            .type(EXPENSE)
            .build();
    private static final Item SALARY = Item.builder()
            .title("Salary")
            .moneyAmount(Money.of(9100, USD))
            .period(MONTH)
            .icon("wallet")
            .type(INCOME)
            .build();
    private static final Saving SAVING = Saving.builder()
            .moneyAmount(Money.of(BigDecimal.ZERO, USD))
            .interest(BigDecimal.ZERO)
            .build();

    @Mock
    private AccountRepository accountRepository;
    private ReactiveKafkaProducerTemplate<String, AccountUpdatedEvent> producerTemplate;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        producerTemplate = spy(new ReactiveKafkaProducerTemplate<>(SenderOptions.create()));
        accountService = new AccountService(TOPIC, accountRepository, producerTemplate);
    }

    /**
     * Test for {@link AccountService#getAccount(Mono)}.
     */
    @Test
    void shouldGetAccount() {
        var account = stubAccount();
        when(accountRepository.getByName(NAME)).thenReturn(Mono.just(account));

        accountService.getAccount(GET_ACCOUNT_REQUEST)
                .as(StepVerifier::create)
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
        when(accountRepository.getByName(NAME)).thenReturn(Mono.empty());

        accountService.getAccount(GET_ACCOUNT_REQUEST)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateAccount() {
        when(accountRepository.update(any(Account.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        doAnswer(invocation -> {
            var record = (SenderRecord<String, AccountUpdatedEvent, AccountServiceProto.Account>) invocation.getArgument(0);
            return Mono.just(new MockSenderResult(record.correlationMetadata()));
        }).when(producerTemplate).send(any(SenderRecord.class));

        var savingAmount = Money.of(1500, USD);
        var interest = BigDecimal.valueOf(3.32);
        var saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(moneyConverter().convert(savingAmount))
                .setInterest(bigDecimalConverter().convert(interest))
                .setDeposit(true)
                .build();

        var rentAmount = Money.of(1200, USD);
        var rent = AccountServiceProto.Item.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle("Rent")
                .setMoney(moneyConverter().convert(rentAmount))
                .setPeriod(TimePeriod.MONTH)
                .setIcon("home")
                .build();

        var mealAmount = Money.of(20, USD);
        var meal = AccountServiceProto.Item.newBuilder()
                .setType(ItemType.EXPENSE)
                .setTitle("Meal")
                .setMoney(moneyConverter().convert(mealAmount))
                .setPeriod(TimePeriod.DAY)
                .setIcon("meal")
                .build();

        var account = AccountServiceProto.Account.newBuilder()
                .setName(NAME)
                .addItems(rent)
                .addItems(meal)
                .setSaving(saving)
                .setNote(NOTE)
                .build();

        accountService.updateAccount(Mono.just(account))
                .as(StepVerifier::create)
                .expectNextMatches(a -> assertAccount(a, saving, rent, meal))
                .verifyComplete();

        verify(accountRepository).update(argThat(a -> {
            assertThat(a.getName()).isEqualTo(NAME);
            assertThat(a.getItems()).extracting(
                    Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon, Item::getType
            ).containsExactlyInAnyOrder(
                    tuple(rent.getTitle(), rentAmount, MONTH, rent.getIcon(), EXPENSE),
                    tuple(meal.getTitle(), mealAmount, DAY, meal.getIcon(), EXPENSE)
            );
            assertThat(a.getSaving().getMoneyAmount()).isEqualTo(savingAmount);
            assertThat(a.getSaving().getInterest()).isEqualTo(interest);
            assertThat(a.getSaving().isDeposit()).isTrue();
            assertThat(a.getSaving().isCapitalization()).isFalse();
            assertThat(a.getNote()).isEqualTo(NOTE);
            return true;
        }));

        verify(producerTemplate)
                .send(ArgumentMatchers.<SenderRecord<String, AccountUpdatedEvent, AccountServiceProto.Account>>argThat(record -> {
                    assertThat(record.key()).isEqualTo(NAME);
                    assertThat(record.value().getAccountName()).isEqualTo(NAME);
                    assertThat(record.value().getItemsList()).containsExactlyInAnyOrder(rent, meal);
                    assertThat(record.value().getSaving()).isEqualTo(saving);
                    assertThat(record.value().getNote()).isEqualTo(NOTE);
                    assertAccount(record.correlationMetadata(), saving, rent, meal);
                    return true;
                }));
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)} when the account to be updated isn't found.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldFailToUpdateAccountWhenNotFound() {
        when(accountRepository.update(any(Account.class))).thenReturn(Mono.empty());

        var savingAmount = Money.of(BigDecimal.ZERO, USD);
        var saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(moneyConverter().convert(savingAmount))
                .setInterest(bigDecimalConverter().convert(BigDecimal.ZERO))
                .build();
        var account = AccountServiceProto.Account.newBuilder()
                .setName(NAME)
                .setSaving(saving)
                .build();

        accountService.updateAccount(Mono.just(account))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });

        verify(producerTemplate, never()).send(any(SenderRecord.class));
    }

    /**
     * Test for {@link AccountService#updateAccount(Mono)} when account data are invalid.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldFailToUpdateAccountWhenDataInvalid() {
        var saving = AccountServiceProto.Saving.newBuilder()
                .setDeposit(true)
                .build();
        var account = AccountServiceProto.Account.newBuilder()
                .setName(NAME)
                .setSaving(saving)
                .build();

        accountService.updateAccount(Mono.just(account))
                .as(StepVerifier::create)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(StatusRuntimeException.class);
                    assertThat(Status.fromThrowable(t).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    return true;
                }).verify();

        verify(accountRepository, never()).update(any());
        verify(producerTemplate, never()).send(any(SenderRecord.class));
    }

    private Account stubAccount() {
        return Account.builder()
                .name(NAME)
                .item(GROCERY)
                .item(SALARY)
                .saving(SAVING)
                .build();
    }

    private boolean assertAccount(AccountServiceProto.Account account, AccountServiceProto.Saving saving,
                                  AccountServiceProto.Item... items) {
        assertThat(account.getName()).isEqualTo(NAME);
        assertThat(account.getItemsList()).containsExactlyInAnyOrder(items);
        assertThat(account.getSaving()).isEqualTo(saving);
        assertThat(account.getUpdateTime().isInitialized()).isTrue();
        assertThat(account.getNote()).isEqualTo(NOTE);
        return true;
    }

    @RequiredArgsConstructor
    private static class MockSenderResult implements SenderResult<AccountServiceProto.Account> {
        private final AccountServiceProto.Account account;

        @Override
        public RecordMetadata recordMetadata() {
            return null;
        }

        @Override
        public Exception exception() {
            return null;
        }

        @Override
        public AccountServiceProto.Account correlationMetadata() {
            return this.account;
        }
    }
}
