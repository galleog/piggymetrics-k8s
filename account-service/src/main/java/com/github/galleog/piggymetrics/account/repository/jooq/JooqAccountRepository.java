package com.github.galleog.piggymetrics.account.repository.jooq;

import static com.github.galleog.piggymetrics.account.domain.Sequences.ITEM_SEQ;
import static com.github.galleog.piggymetrics.account.domain.Tables.ACCOUNTS;
import static com.github.galleog.piggymetrics.account.domain.Tables.ITEMS;
import static com.github.galleog.piggymetrics.account.domain.Tables.SAVINGS;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.domain.tables.records.AccountsRecord;
import com.github.galleog.piggymetrics.account.domain.tables.records.ItemsRecord;
import com.github.galleog.piggymetrics.account.domain.tables.records.SavingsRecord;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link AccountRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqAccountRepository implements AccountRepository {
    private final DSLContext dsl;

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> getByName(@NonNull String name) {
        Validate.notNull(name);

        Result<Record> records = dsl.select()
                .from(ACCOUNTS)
                .leftJoin(ITEMS).on(ITEMS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                .join(SAVINGS).on(SAVINGS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                .where(ACCOUNTS.NAME.eq(name))
                .fetch();
        return records.isEmpty() ? Optional.empty() : Optional.of(toAccount(records));
    }

    @NonNull
    @Override
    @Transactional
    public Account save(@NonNull Account account) {
        Validate.notNull(account);

        AccountsRecord accountsRecord = dsl.newRecord(ACCOUNTS);
        accountsRecord.from(account);
        accountsRecord.setUpdateTime(LocalDateTime.now());
        accountsRecord.insert();

        SavingsRecord savingsRecord = dsl.newRecord(SAVINGS);
        savingsRecord.setAccountName(account.getName());
        copy(savingsRecord, account.getSaving());
        savingsRecord.insert();

        List<ItemsRecord> itemsRecords = insertItems(account);

        List<Item> items = itemsRecords.stream()
                .map(rec -> rec.into(Item.class))
                .collect(ImmutableList.toImmutableList());
        Saving saving = savingsRecord.into(Saving.class);
        return Account.builder()
                .name(accountsRecord.getName())
                .items(items)
                .saving(saving)
                .note(accountsRecord.getNote())
                .updateTime(accountsRecord.getUpdateTime())
                .build();
    }

    @Override
    @Transactional
    public Optional<Account> update(@NonNull Account account) {
        Validate.notNull(account);

        Record record = dsl.select()
                .from(ACCOUNTS)
                .join(SAVINGS).on(SAVINGS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                .where(ACCOUNTS.NAME.eq(account.getName()))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }

        AccountsRecord accountsRecord = record.into(ACCOUNTS);
        accountsRecord.setNote(account.getNote());
        accountsRecord.setUpdateTime(LocalDateTime.now());
        accountsRecord.update();

        SavingsRecord savingsRecord = record.into(SAVINGS);
        copy(savingsRecord, account.getSaving());
        savingsRecord.update();

        dsl.deleteFrom(ITEMS)
                .where(ITEMS.ACCOUNT_NAME.eq(account.getName()))
                .execute();
        List<ItemsRecord> itemsRecords = insertItems(account);

        List<Item> items = itemsRecords.stream()
                .map(rec -> rec.into(Item.class))
                .collect(ImmutableList.toImmutableList());
        Saving saving = savingsRecord.into(Saving.class);
        return Optional.of(Account.builder()
                .name(accountsRecord.getName())
                .items(items)
                .saving(saving)
                .note(accountsRecord.getNote())
                .updateTime(accountsRecord.getUpdateTime())
                .build());
    }

    private Account toAccount(Result<Record> records) {
        Record record = records.get(0);

        List<Item> items = records.stream()
                .filter(r -> r.get(ITEMS.ID) != null)
                .map(r -> r.into(Item.class))
                .collect(ImmutableList.toImmutableList());
        Saving saving = record.into(Saving.class);
        return Account.builder()
                .name(record.get(ACCOUNTS.NAME))
                .items(items)
                .saving(saving)
                .note(record.get(ACCOUNTS.NOTE))
                .updateTime(record.get(ACCOUNTS.UPDATE_TIME))
                .build();
    }

    private void copy(SavingsRecord record, Saving saving) {
        record.from(saving);
        record.setMoneyAmount(saving.getMoneyAmount().getNumber().numberValue(BigDecimal.class));
        record.setCurrencyCode(saving.getMoneyAmount().getCurrency().getCurrencyCode());
    }

    private List<ItemsRecord> insertItems(Account account) {
        if (CollectionUtils.isEmpty(account.getItems())) {
            return ImmutableList.of();
        }

        List<ItemsRecord> records = account.getItems().stream()
                .map(item -> {
                    ItemsRecord record = dsl.newRecord(ITEMS);
                    record.from(item);
                    record.setId(dsl.nextval(ITEM_SEQ));
                    record.setAccountName(account.getName());
                    record.setMoneyAmount(item.getMoneyAmount().getNumber().numberValue(BigDecimal.class));
                    record.setCurrencyCode(item.getMoneyAmount().getCurrency().getCurrencyCode());
                    record.setItemType(item.getType());
                    return record;
                }).collect(ImmutableList.toImmutableList());
        dsl.batchInsert(records).execute();
        return records;
    }
}
