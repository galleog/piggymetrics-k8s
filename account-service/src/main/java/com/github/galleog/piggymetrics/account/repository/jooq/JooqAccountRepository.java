package com.github.galleog.piggymetrics.account.repository.jooq;

import static com.github.galleog.piggymetrics.account.domain.Sequences.ITEM_SEQ;
import static com.github.galleog.piggymetrics.account.domain.Tables.ACCOUNTS;
import static com.github.galleog.piggymetrics.account.domain.Tables.ITEMS;
import static com.github.galleog.piggymetrics.account.domain.Tables.SAVINGS;
import static org.jooq.impl.DSL.val;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.domain.tables.records.AccountsRecord;
import com.github.galleog.piggymetrics.account.domain.tables.records.SavingsRecord;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.r2dbc.jooq.transaction.TransactionAwareJooqWrapper;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link AccountRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqAccountRepository implements AccountRepository {
    private final TransactionAwareJooqWrapper wrapper;

    @Override
    @Transactional(readOnly = true)
    public Mono<Account> getByName(@NonNull String name) {
        Validate.notNull(name);
        return wrapper.withDSLContextMany(ctx ->
                        ctx.select()
                                .from(ACCOUNTS)
                                .leftJoin(ITEMS).on(ITEMS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                                .join(SAVINGS).on(SAVINGS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                                .where(ACCOUNTS.NAME.eq(name))
                ).collectList()
                .mapNotNull(this::toAccount);
    }

    @Override
    @Transactional
    public Mono<Account> save(@NonNull Account account) {
        Validate.notNull(account);
        return insertAccountSql(account)
                .map(record ->
                        Account.builder()
                                .name(record.getName())
                                .note(record.getNote())
                                .updateTime(record.getUpdateTime())
                ).flatMap(builder ->
                        insertSavingSql(account)
                                .map(record -> builder.saving(toSaving(record)))
                ).flatMap(builder ->
                        insertItemsRecords(account)
                                .map(items -> builder.items(items).build())
                );
    }

    @Override
    @Transactional
    public Mono<Account> update(@NonNull Account account) {
        Validate.notNull(account);
        return updateAccountSql(account)
                .map(record ->
                        Account.builder()
                                .name(record.getName())
                                .note(record.getNote())
                                .updateTime(record.getUpdateTime())
                ).flatMap(builder ->
                        updateSavingSql(account)
                                .map(record -> builder.saving(toSaving(record)))
                ).flatMap(builder ->
                        deleteItemsSql(account.getName())
                                .map(i -> builder)
                ).flatMap(builder ->
                        insertItemsRecords(account)
                                .map(items -> builder.items(items).build())
                );
    }

    private Mono<AccountsRecord> insertAccountSql(Account account) {
        return wrapper.withDSLContext(ctx ->
                ctx.insertInto(ACCOUNTS)
                        .columns(ACCOUNTS.NAME, ACCOUNTS.NOTE, ACCOUNTS.UPDATE_TIME)
                        .values(account.getName(), account.getNote(), LocalDateTime.now())
                        .returning()
        );
    }

    private Mono<AccountsRecord> updateAccountSql(Account account) {
        return wrapper.withDSLContext(ctx ->
                ctx.update(ACCOUNTS)
                        .set(ACCOUNTS.NOTE, account.getNote())
                        .set(ACCOUNTS.UPDATE_TIME, LocalDateTime.now())
                        .where(ACCOUNTS.NAME.eq(account.getName()))
                        .returning()
        );
    }

    private Mono<SavingsRecord> insertSavingSql(Account account) {
        return wrapper.withDSLContext(ctx ->
                ctx.insertInto(SAVINGS)
                        .columns(
                                SAVINGS.ACCOUNT_NAME,
                                SAVINGS.CURRENCY_CODE,
                                SAVINGS.MONEY_AMOUNT,
                                SAVINGS.INTEREST,
                                SAVINGS.DEPOSIT,
                                SAVINGS.CAPITALIZATION
                        ).values(
                                account.getName(),
                                account.getSaving().getMoneyAmount().getCurrency().getCurrencyCode(),
                                account.getSaving().getMoneyAmount().getNumber().numberValue(BigDecimal.class),
                                account.getSaving().getInterest(),
                                account.getSaving().isDeposit(),
                                account.getSaving().isCapitalization()
                        ).returning()
        );
    }

    private Mono<SavingsRecord> updateSavingSql(Account account) {
        return wrapper.withDSLContext(ctx ->
                ctx.update(SAVINGS)
                        .set(SAVINGS.CURRENCY_CODE, account.getSaving().getMoneyAmount().getCurrency().getCurrencyCode())
                        .set(SAVINGS.MONEY_AMOUNT, account.getSaving().getMoneyAmount().getNumber().numberValue(BigDecimal.class))
                        .set(SAVINGS.INTEREST, account.getSaving().getInterest())
                        .set(SAVINGS.DEPOSIT, account.getSaving().isDeposit())
                        .set(SAVINGS.CAPITALIZATION, account.getSaving().isCapitalization())
                        .where(SAVINGS.ACCOUNT_NAME.eq(account.getName()))
                        .returning()
        );
    }

    private Mono<List<Item>> insertItemsRecords(Account account) {
        return Flux.fromIterable(account.getItems())
                .flatMap(item -> wrapper.withDSLContext(ctx ->
                        ctx.insertInto(ITEMS)
                                .columns(
                                        ITEMS.ID,
                                        ITEMS.ACCOUNT_NAME,
                                        ITEMS.TITLE,
                                        ITEMS.CURRENCY_CODE,
                                        ITEMS.MONEY_AMOUNT,
                                        ITEMS.PERIOD,
                                        ITEMS.ICON,
                                        ITEMS.ITEM_TYPE
                                ).values(
                                        ITEM_SEQ.nextval(),
                                        val(account.getName()),
                                        val(item.getTitle()),
                                        val(item.getMoneyAmount().getCurrency().getCurrencyCode()),
                                        val(item.getMoneyAmount().getNumber().numberValue(BigDecimal.class)),
                                        val(item.getPeriod()),
                                        val(item.getIcon()),
                                        val(item.getType())
                                ).returning()
                )).map(this::toItem)
                .collectList();
    }

    private Mono<Integer> deleteItemsSql(String account) {
        return wrapper.withDSLContext(ctx ->
                ctx.deleteFrom(ITEMS)
                        .where(ITEMS.ACCOUNT_NAME.eq(account))
        );
    }

    private Account toAccount(List<Record> records) {
        if (records.isEmpty()) {
            return null;
        }

        var record = records.get(0);

        var items = records.stream()
                .filter(r -> r.get(ITEMS.ID) != null)
                .map(this::toItem)
                .collect(ImmutableList.toImmutableList());
        var saving = toSaving(record);
        return Account.builder()
                .name(record.get(ACCOUNTS.NAME))
                .items(items)
                .saving(saving)
                .note(record.get(ACCOUNTS.NOTE))
                .updateTime(record.get(ACCOUNTS.UPDATE_TIME))
                .build();
    }

    private Saving toSaving(Record record) {
        return Saving.builder()
                .moneyAmount(Money.of(
                        record.get(SAVINGS.MONEY_AMOUNT),
                        record.get(SAVINGS.CURRENCY_CODE)
                )).interest(record.get(SAVINGS.INTEREST))
                .deposit(record.get(SAVINGS.DEPOSIT))
                .capitalization(record.get(SAVINGS.CAPITALIZATION))
                .build();
    }

    private Item toItem(Record record) {
        return Item.builder()
                .id(record.get(ITEMS.ID))
                .title(record.get(ITEMS.TITLE))
                .moneyAmount(Money.of(
                        record.get(ITEMS.MONEY_AMOUNT),
                        record.get(ITEMS.CURRENCY_CODE)
                )).period(record.get(ITEMS.PERIOD))
                .icon(record.get(ITEMS.ICON))
                .type(record.get(ITEMS.ITEM_TYPE))
                .build();
    }
}
