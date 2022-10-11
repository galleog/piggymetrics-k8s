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
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.GetAccountRequest;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.google.common.base.Converter;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

import javax.money.MonetaryException;
import java.time.DateTimeException;
import java.util.stream.Collectors;

/**
 * Service to work with accounts.
 */
@Slf4j
@GrpcService
public class AccountService extends ReactorAccountServiceGrpc.AccountServiceImplBase {
    private static final Converter<Account, AccountServiceProto.Account> ACCOUNT_CONVERTER = new AccountConverter();
    private static final Converter<Item, AccountServiceProto.Item> ITEM_CONVERTER = new ItemConverter();
    private static final Converter<Saving, AccountServiceProto.Saving> SAVING_CONVERTER = new SavingConverter();

    private final String topic;
    private final AccountRepository accountRepository;
    private final ReactiveKafkaProducerTemplate<String, AccountUpdatedEvent> producerTemplate;

    /**
     * Constructs an object instance.
     */
    public AccountService(@Value("${spring.kafka.producer.topic}") String topic, AccountRepository accountRepository,
                          ReactiveKafkaProducerTemplate<String, AccountUpdatedEvent> producerTemplate) {
        this.topic = topic;
        this.accountRepository = accountRepository;
        this.producerTemplate = producerTemplate;
    }

    @Override
    public Mono<AccountServiceProto.Account> getAccount(Mono<GetAccountRequest> request) {
        return request.map(GetAccountRequest::getName)
                .flatMap(this::doGetAccount)
                .map(ACCOUNT_CONVERTER::convert);
    }

    @Override
    @Transactional
    public Mono<AccountServiceProto.Account> updateAccount(Mono<AccountServiceProto.Account> request) {
        return request.map(account -> ACCOUNT_CONVERTER.reverse().convert(account))
                .flatMap(this::doUpdateAccount);
    }

    private Mono<Account> doGetAccount(String name) {
        return accountRepository.getByName(name)
                .doOnNext(account -> logger.debug("Account for user '{}' found", name))
                .switchIfEmpty(Mono.error(() -> Status.NOT_FOUND
                        .withDescription("Account for user '" + name + "' not found")
                        .asRuntimeException()));
    }

    private Mono<AccountServiceProto.Account> doUpdateAccount(Account account) {
        return accountRepository.update(account)
                .switchIfEmpty(Mono.error(() -> Status.NOT_FOUND
                        .withDescription("Account for user '" + account.getName() + "' not found")
                        .asRuntimeException()))
                .map(ACCOUNT_CONVERTER::convert)
                .flatMap(this::sendEvent)
                .map(SenderResult::correlationMetadata)
                .doOnNext(a -> logger.info("Account for user '{}' updated", a.getName()));
    }

    private Mono<SenderResult<AccountServiceProto.Account>> sendEvent(AccountServiceProto.Account account) {
        var event = AccountUpdatedEvent.newBuilder()
                .setAccountName(account.getName())
                .addAllItems(account.getItemsList())
                .setSaving(account.getSaving())
                .setNote(account.getNote())
                .build();
        var record = new ProducerRecord<>(topic, account.getName(), event);
        return producerTemplate.send(SenderRecord.create(record, account))
                .doOnError(e -> logger.error("Failed to send AccountUpdatedEvent for user'" + account.getName() + "'", e));
    }

    private static final class AccountConverter extends Converter<Account, AccountServiceProto.Account> {
        @Override
        @NonNull
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
        @NonNull
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
            } catch (NullPointerException | IllegalArgumentException | DateTimeException | ArithmeticException |
                     MonetaryException e) {
                throw Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .withCause(e)
                        .asRuntimeException();
            }
        }
    }

    private static final class ItemConverter extends Converter<Item, AccountServiceProto.Item> {
        @Override
        @NonNull
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
        @NonNull
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
        @NonNull
        protected AccountServiceProto.Saving doForward(Saving saving) {
            return AccountServiceProto.Saving.newBuilder()
                    .setMoney(moneyConverter().convert(saving.getMoneyAmount()))
                    .setInterest(bigDecimalConverter().convert(saving.getInterest()))
                    .setDeposit(saving.isDeposit())
                    .setCapitalization(saving.isCapitalization())
                    .build();
        }

        @Override
        @NonNull
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
