package com.github.galleog.piggymetrics.account.event;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Consumer of events on new user registrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer implements Consumer<UserRegisteredEvent> {
    @VisibleForTesting
    public static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void accept(UserRegisteredEvent event) {
        logger.info("UserRegisteredEvent for user '{}' received", event.getUserName());

        if (accountRepository.getByName(event.getUserName()).isPresent()) {
            logger.warn("Account for user '{}' already exists", event.getUserName());
            return;
        }

        Saving saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.ZERO, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .deposit(false)
                .capitalization(false)
                .build();
        Account account = Account.builder()
                .name(event.getUserName())
                .saving(saving)
                .build();
        accountRepository.save(account);

        logger.info("Account for user '{}' created", event.getUserName());
    }
}
