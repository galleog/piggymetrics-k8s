package com.github.galleog.piggymetrics.account.service;

import com.github.galleog.piggymetrics.account.acl.AccountDto;
import com.github.galleog.piggymetrics.account.acl.User;
import com.github.galleog.piggymetrics.account.client.AuthServiceClient;
import com.github.galleog.piggymetrics.account.client.StatisticsServiceClient;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.Collection;
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
    public static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

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
                .moneyAmount(Money.of(0, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .deposit(false)
                .capitalization(false)
                .build();
        Account account = Account.builder()
                .name(user.getUsername())
                .saving(saving)
                .build();
        account = repository.save(account);
        logger.info("New account {} created", account.getName());

        return account;
    }

    /**
     * Updates the account specified by its name.
     * <p/>
     * The method updates statistics.
     *
     * @param name   the name of the account to be updated
     * @param items  the new items
     * @param saving the new saving
     * @param note   the optional new note
     * @return the updated account or {@link Optional#empty()} if there is no account with the given name
     * @throws NullPointerException     if the name or the new account data is {@code null}
     * @throws IllegalArgumentException if the name is blank
     * @see StatisticsServiceClient#updateStatistics(String, AccountDto)
     */
    @Transactional
    public Optional<Account> update(@NonNull String name, @NonNull Collection<Item> items,
                                    @NonNull Saving saving, @Nullable String note) {
        Validate.notBlank(name);
        Validate.noNullElements(items);
        Validate.notNull(saving);

        Optional<Account> optional = repository.findByName(name);
        optional.ifPresent(account -> {
            account.update(items, saving, note);
            repository.save(account);
            logger.info("Account {} updated", name);

            statisticsClient.updateStatistics(name, AccountDto.builder()
                    .incomes(account.getIncomes())
                    .expenses(account.getExpenses())
                    .saving(account.getSaving())
                    .build());
        });
        return optional;
    }
}
