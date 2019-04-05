package com.github.galleog.piggymetrics.account.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.timestampConverter;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.ItemType;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.domain.TimePeriod;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Service to work with accounts.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AccountService extends ReactorAccountServiceGrpc.AccountServiceImplBase {
    @VisibleForTesting
    public static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    private static final Converter<Item, AccountServiceProto.Item> ITEM_CONVERTER = new ItemConverter();
    private static final Converter<Saving, AccountServiceProto.Saving> SAVING_CONVERTER = new SavingConverter();

    @Qualifier("jdbcScheduler")
    private final Scheduler jdbcScheduler;
    private final AccountRepository repository;

    @Override
    public Mono<AccountServiceProto.Account> getAccount(Mono<AccountServiceProto.GetAccountRequest> request) {
        return request.flatMap(req ->
                async(() -> repository.findByName(req.getName()))
                        .switchIfEmpty(Mono.error(
                                Status.NOT_FOUND
                                        .withDescription("Account for user " + req.getName() + " not found")
                                        .asRuntimeException()
                        )).doOnNext(a -> logger.debug("Account <{}> found", a))
        ).map(this::toAccountProto);
    }

    @Override
    public Mono<AccountServiceProto.Account> createAccount(Mono<AccountServiceProto.CreateAccountRequest> request) {
        return request.flatMap(req ->
                async(() -> createNewAccount(req.getName()))
        ).map(this::toAccountProto);
    }

    @Override
    public Mono<AccountServiceProto.Account> updateAccount(Mono<AccountServiceProto.Account> request) {
        return request.flatMap(account ->
                async(() -> updateAccountInternal(account))
        ).map(this::toAccountProto);
    }

    @Transactional
    private Account createNewAccount(String name) {
        if (repository.findByName(name) != null) {
            throw Status.ALREADY_EXISTS
                    .withDescription("Account " + name + " already exists")
                    .asRuntimeException();
        }

        Saving saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.ZERO, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .deposit(false)
                .capitalization(false)
                .build();
        Account account = Account.builder()
                .name(name)
                .saving(saving)
                .build();
        repository.save(account);
        logger.info("Account for user {} created", name);

        return account;
    }

    @Transactional
    private Account updateAccountInternal(AccountServiceProto.Account account) {
//        String name = account.getName();
//        Account updated = repository.findByName(name)
//                .orElseThrow(() -> Status.NOT_FOUND
//                        .withDescription("Account for user '" + name + "' not found")
//                        .asRuntimeException());
//        try {
//            updated.update(
//                    account.getItemsList().stream()
//                            .map(item -> ITEM_CONVERTER.reverse().convert(item))
//                            .collect(Collectors.toList()),
//                    SAVING_CONVERTER.reverse().convert(account.getSaving()),
//                    account.getNote()
//            );
//        } catch (NullPointerException | IllegalArgumentException e) {
//            throw Status.INVALID_ARGUMENT
//                    .withDescription(e.getMessage())
//                    .withCause(e)
//                    .asRuntimeException();
//        }
//
//        repository.save(updated);
//        logger.info("Account {} updated", name);

//            statisticsClient.updateStatistics(name, AccountDto.builder()
//                    .incomes(account.getIncomes())
//                    .expenses(account.getExpenses())
//                    .saving(account.getSaving())
//                    .build());

        return null;
    }

    private <T> Mono<T> async(Callable<? extends T> supplier) {
        return Mono.<T>fromCallable(supplier)
                .subscribeOn(jdbcScheduler);
    }

    private AccountServiceProto.Account toAccountProto(Account account) {
        AccountServiceProto.Account.Builder builder = AccountServiceProto.Account.newBuilder()
                .setName(account.getName())
                .addAllItems(account.getItems().stream()
                        .map(ITEM_CONVERTER::convert)
                        .collect(Collectors.toList()))
                .setSaving(SAVING_CONVERTER.convert(account.getSaving()))
                .setUpdateTime(timestampConverter().convert(account.getUpdateTime()));
        if (account.getNote() != null) {
            builder.setNote(account.getNote());
        }
        return builder.build();
    }

    private static final class ItemConverter extends Converter<Item, AccountServiceProto.Item> {
        @Override
        protected AccountServiceProto.Item doForward(Item item) {
            return AccountServiceProto.Item.newBuilder()
                    .setTitle(item.getTitle())
                    .setMoney(moneyConverter().convert(item.getMoneyAmount()))
                    .setPeriod(AccountServiceProto.Item.TimePeriod.valueOf(item.getPeriod().name()))
                    .setIcon(item.getIcon())
                    .setType(AccountServiceProto.Item.ItemType.valueOf(item.getType().name()))
                    .build();
        }

        @Override
        protected Item doBackward(AccountServiceProto.Item item) {
            return Item.builder()
                    .title(item.getTitle())
                    .moneyAmount(moneyConverter().reverse().convert(item.getMoney()))
                    .period(TimePeriod.valueOf(item.getPeriod().name()))
                    .icon(item.getIcon())
                    .type(ItemType.valueOf(item.getType().name()))
                    .build();
        }
    }

    private static final class SavingConverter extends Converter<Saving, AccountServiceProto.Saving> {
        @Override
        protected AccountServiceProto.Saving doForward(Saving saving) {
            return AccountServiceProto.Saving.newBuilder()
                    .setMoney(moneyConverter().convert(saving.getMoneyAmount()))
                    .setInterest(bigDecimalConverter().convert(saving.getInterest()))
                    .setDeposit(saving.isDeposit())
                    .setCapitalization(saving.isCapitalization())
                    .build();
        }

        @Override
        protected Saving doBackward(AccountServiceProto.Saving saving) {
            return Saving.builder()
                    .moneyAmount(moneyConverter().reverse().convert(saving.getMoney()))
                    .interest(bigDecimalConverter().reverse().convert(saving.getInterest()))
                    .deposit(saving.getDeposit())
                    .capitalization(saving.getCapitalization())
                    .build();
        }
    }
}
