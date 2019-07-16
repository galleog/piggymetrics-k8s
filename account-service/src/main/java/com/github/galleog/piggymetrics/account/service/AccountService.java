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
import com.google.common.base.Converter;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.lang.NonNull;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.money.MonetaryException;
import java.time.DateTimeException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Service to work with accounts.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AccountService extends ReactorAccountServiceGrpc.AccountServiceImplBase {
    private static final Converter<Account, AccountServiceProto.Account> ACCOUNT_CONVERTER = new AccountConverter();
    private static final Converter<Item, AccountServiceProto.Item> ITEM_CONVERTER = new ItemConverter();
    private static final Converter<Saving, AccountServiceProto.Saving> SAVING_CONVERTER = new SavingConverter();

    private final Scheduler jdbcScheduler;
    private final Source source;
    private final AccountRepository repository;

    @Override
    public Mono<AccountServiceProto.Account> getAccount(Mono<AccountServiceProto.GetAccountRequest> request) {
        return request.flatMap(req ->
                async(() ->
                        repository.getByName(req.getName())
                                .orElseThrow(() -> Status.NOT_FOUND
                                        .withDescription("Account for user '" + req.getName() + "' not found")
                                        .asRuntimeException())
                ).doOnNext(account -> logger.debug("Account for user '{}' found", account))
        ).map(ACCOUNT_CONVERTER::convert);
    }

    @Override
    public Mono<AccountServiceProto.Account> updateAccount(Mono<AccountServiceProto.Account> request) {
        return request.flatMap(account -> async(() -> doUpdateAccount(account)))
                .map(ACCOUNT_CONVERTER::convert);
    }

    private Account doUpdateAccount(AccountServiceProto.Account account) {
        Account updated = repository.update(ACCOUNT_CONVERTER.reverse().convert(account))
                .orElseThrow(() -> Status.NOT_FOUND
                        .withDescription("Account for user '" + account.getName() + "' not found")
                        .asRuntimeException());

        // send an AccountUpdated event
        AccountServiceProto.AccountUpdatedEvent event = AccountServiceProto.AccountUpdatedEvent.newBuilder()
                .setAccountName(account.getName())
                .addAllItems(account.getItemsList())
                .setSaving(account.getSaving())
                .build();
        source.output().send(MessageBuilder.withPayload(event).build());

        logger.info("Account for user '{}' updated", account.getName());
        return updated;
    }

    private <T> Mono<T> async(Callable<? extends T> supplier) {
        return Mono.<T>fromCallable(supplier)
                .subscribeOn(jdbcScheduler);
    }

    private static final class AccountConverter extends Converter<Account, AccountServiceProto.Account> {
        @Override
        protected AccountServiceProto.Account doForward(@NonNull Account account) {
            AccountServiceProto.Account.Builder builder = AccountServiceProto.Account.newBuilder()
                    .setName(account.getName())
                    .addAllItems(account.getItems().stream()
                            .map(ITEM_CONVERTER::convert)
                            .collect(Collectors.toList()))
                    .setSaving(SAVING_CONVERTER.convert(account.getSaving()));
            if (account.getUpdateTime() != null) {
                builder.setUpdateTime(timestampConverter().convert(account.getUpdateTime()));
            }
            if (account.getNote() != null) {
                builder.setNote(account.getNote());
            }
            return builder.build();
        }

        @Override
        protected Account doBackward(@NonNull AccountServiceProto.Account account) {
            try {
                Account.AccountBuilder builder = Account.builder()
                        .name(account.getName())
                        .saving(SAVING_CONVERTER.reverse().convert(account.getSaving()))
                        .items(account.getItemsList().stream()
                                .map(item -> ITEM_CONVERTER.reverse().convert(item))
                                .collect(Collectors.toList()));
                if (account.hasUpdateTime()) {
                    builder.updateTime(timestampConverter().reverse().convert(account.getUpdateTime()));
                }
                if (StringUtils.isNotEmpty(account.getNote())) {
                    builder.note(account.getNote());
                }
                return builder.build();
            } catch (NullPointerException | IllegalArgumentException | DateTimeException | ArithmeticException | MonetaryException e) {
                throw Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .withCause(e)
                        .asRuntimeException();
            }
        }
    }

    private static final class ItemConverter extends Converter<Item, AccountServiceProto.Item> {
        @Override
        protected AccountServiceProto.Item doForward(@NonNull Item item) {
            return AccountServiceProto.Item.newBuilder()
                    .setTitle(item.getTitle())
                    .setMoney(moneyConverter().convert(item.getMoneyAmount()))
                    .setPeriod(AccountServiceProto.TimePeriod.valueOf(item.getPeriod().name()))
                    .setIcon(item.getIcon())
                    .setType(AccountServiceProto.ItemType.valueOf(item.getType().name()))
                    .build();
        }

        @Override
        protected Item doBackward(@NonNull AccountServiceProto.Item item) {
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
