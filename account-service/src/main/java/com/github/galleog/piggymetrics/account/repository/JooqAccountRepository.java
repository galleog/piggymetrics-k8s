package com.github.galleog.piggymetrics.account.repository;

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
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation of {@link AccountRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqAccountRepository implements AccountRepository {
    private final DSLContext dsl;

    @Override
    @Transactional(readOnly = true)
    public Account findByName(@NonNull String name) {
        Validate.notBlank(name);

        Result<Record> records = dsl.select()
                .from(ACCOUNTS)
                .leftJoin(ITEMS).on(ITEMS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                .join(SAVINGS).on(SAVINGS.ACCOUNT_NAME.eq(ACCOUNTS.NAME))
                .where(ACCOUNTS.NAME.eq(name))
                .fetch();
        return records.isEmpty() ? null : toAccount(records);
    }

    @Override
    @Transactional
    public void save(@NonNull Account account) {
        Validate.notNull(account);

        AccountsRecord accountsRecord = dsl.newRecord(ACCOUNTS);
        accountsRecord.from(account);

        accountsRecord.insert();

        SavingsRecord savingsRecord = dsl.newRecord(SAVINGS);
        savingsRecord.from(account.getSaving());
        savingsRecord.setAccountName(account.getName());
        savingsRecord.setMoneyAmount(account.getSaving().getMoneyAmount().getNumber().numberValue(BigDecimal.class));
        savingsRecord.setCurrencyCode(account.getSaving().getMoneyAmount().getCurrency().getCurrencyCode());
        savingsRecord.insert();

        List<ItemsRecord> records = account.getItems().stream()
                .map(item -> {
                    ItemsRecord itemsRecord = dsl.newRecord(ITEMS);
                    itemsRecord.from(item);
                    itemsRecord.setId(dsl.nextval(ITEM_SEQ));
                    itemsRecord.setAccountName(account.getName());
                    itemsRecord.setMoneyAmount(item.getMoneyAmount().getNumber().numberValue(BigDecimal.class));
                    itemsRecord.setCurrencyCode(item.getMoneyAmount().getCurrency().getCurrencyCode());
                    itemsRecord.setItemType(item.getType());
                    return itemsRecord;
                }).collect(ImmutableList.toImmutableList());
        dsl.batchInsert(records).execute();
    }

    private Account toAccount(Result<Record> records) {
        Record record = records.get(0);

        List<Item> items = records.stream()
                .filter(r -> r.get(ITEMS.ID) != null)
                .map(r -> r.into(Item.class))
                .collect(ImmutableList.toImmutableList());
        Saving saving = record.into(Saving.class);
        return Account.builder()
                .name(record.getValue(ACCOUNTS.NAME))
                .items(items)
                .saving(saving)
                .note(record.getValue(ACCOUNTS.NOTE))
                .updateTime(record.getValue(ACCOUNTS.UPDATE_TIME))
                .build();
    }
}
