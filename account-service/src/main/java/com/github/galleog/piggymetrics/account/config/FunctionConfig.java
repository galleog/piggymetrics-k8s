package com.github.galleog.piggymetrics.account.config;

import com.github.galleog.piggymetrics.account.event.UserRegisteredEventConsumer;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Configuration for Spring Cloud Stream functions.
 */
@Configuration(proxyBeanMethods = false)
public class FunctionConfig {
    @Bean
    Sinks.Many<AccountServiceProto.Account> sink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    Supplier<Flux<AccountServiceProto.AccountUpdatedEvent>> accountUpdatedEventSupplier(
            Sinks.Many<AccountServiceProto.Account> sink) {
        return () -> sink.asFlux()
                .map(account -> AccountServiceProto.AccountUpdatedEvent.newBuilder()
                        .setAccountName(account.getName())
                        .addAllItems(account.getItemsList())
                        .setSaving(account.getSaving())
                        .setNote(account.getNote())
                        .build());
    }

    @Bean
    Function<Flux<UserRegisteredEventProto.UserRegisteredEvent>, Mono<Void>> userRegisteredEventConsumer(
            AccountRepository repository, TransactionalOperator operator) {
        return new UserRegisteredEventConsumer(repository, operator);
    }
}
