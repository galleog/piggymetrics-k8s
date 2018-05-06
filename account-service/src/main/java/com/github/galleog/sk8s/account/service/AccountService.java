package com.github.galleog.sk8s.account.service;

import com.github.galleog.sk8s.account.client.AuthServiceClient;
import com.github.galleog.sk8s.account.client.StatisticsServiceClient;
import com.github.galleog.sk8s.account.domain.Account;
import com.github.galleog.sk8s.account.domain.AccountBalance;
import com.github.galleog.sk8s.account.domain.Saving;
import com.github.galleog.sk8s.account.domain.User;
import com.github.galleog.sk8s.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service to work with accounts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    /**
     * Default currency is USD.
     */
    public static final CurrencyUnit DEFAULT_CURRENCY = Monetary.getCurrency("USD");

    private final StatisticsServiceClient statisticsClient;
    private final AuthServiceClient authClient;
    private final AccountRepository repository;

    /**
     * Finds an account by its name.
     *
     * @param name the account name
     * @return the found account, or {@link Optional#empty()} if there is no account with the given name
     * @throws NullPointerException     if the name is {@code null}
     * @throws IllegalArgumentException if the name is blank
     */
    public Optional<Account> findByName(@NonNull String name) {
        Validate.notBlank(name);
        return repository.findByName(name);
    }

    /**
     * Creates a new account with the name of the specified user and default parameters.
     * <p/>
     * The given user will be created using the authentication service.
     *
     * @param user the user to create
     * @return the created account
     * @throws NullPointerException     if the user is {@code null}
     * @throws IllegalArgumentException if an account for the given user already exists
     * @see AuthServiceClient#createUser(User)
     */
    @NonNull
    @Transactional
    public Account create(@NonNull User user) {
        Validate.notNull(user);
        repository.findByName(user.getUsername()).ifPresent(account -> {
            throw new IllegalArgumentException("Account " + user.getUsername() + " already exists");
        });

        authClient.createUser(user);
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(0, DEFAULT_CURRENCY))
                .interest(BigDecimal.ZERO)
                .deposit(false)
                .capitalization(false)
                .build();
        AccountBalance balance = AccountBalance.builder()
                .saving(saving)
                .build();
        Account account = Account.builder()
                .name(user.getUsername())
                .balance(balance)
                .build();
        account = repository.save(account);
        logger.info("New account {} has been created", account.getName());

        return account;
    }

    /**
     * Updates the account specified by its name.
     * <p/>
     * The method updates statistics.
     *
     * @param name   the name of the account to be updated
     * @param update the new account data
     * @return the updated account or {@link Optional#empty()} if there is no account with the given name
     * @throws NullPointerException     if the name or the new account data is {@code null}
     * @throws IllegalArgumentException if the name is blank
     * @see StatisticsServiceClient#updateStatistics(String, AccountBalance)
     */
    @Transactional
    public Optional<Account> update(@NonNull String name, @NonNull Account update) {
        Validate.notBlank(name);
        Validate.notNull(update);

        Optional<Account> account = repository.findByName(name);
        account.ifPresent(accnt -> {
            accnt.update(update);
            repository.save(accnt);
            logger.info("Account {} has been updated", name);

            statisticsClient.updateStatistics(name, accnt.getBalance());
        });
        return account;
    }
}
