package com.github.galleog.piggymetrics.account.event;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Consumer of events on new user registrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer implements Function<Flux<ConsumerRecord<String, UserRegisteredEvent>>, Mono<Void>> {
    @VisibleForTesting
    public static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    private final AccountRepository accountRepository;
    private final TransactionalOperator operator;

    @Override
    public Mono<Void> apply(Flux<ConsumerRecord<String, UserRegisteredEvent>> records) {
        return records.map(record -> record.value().getUserName())
                .doOnNext(name -> logger.info("UserRegisteredEvent for user '{}' received", name))
                .flatMap(this::doCreateAccount)
                .then();
    }

    private Mono<Account> doCreateAccount(String name) {
        return accountRepository.getByName(name)
                .doOnNext(account -> logger.warn("Account for user '{}' already exists", name))
                .hasElement()
                .filter(b -> !b)
                .map(b -> newAccount(name))
                .flatMap(accountRepository::save)
                .doOnNext(account -> logger.info("Account for user '{}' created", account.getName()))
                .as(operator::transactional);
    }

    private Account newAccount(String name) {
        var saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.ZERO, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .deposit(false)
                .capitalization(false)
                .build();
        return Account.builder()
                .name(name)
                .saving(saving)
                .build();
    }
}
