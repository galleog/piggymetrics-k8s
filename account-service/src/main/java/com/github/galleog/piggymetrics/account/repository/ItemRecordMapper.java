package com.github.galleog.piggymetrics.account.repository;

import static com.github.galleog.piggymetrics.account.domain.Tables.ITEMS;

import com.github.galleog.piggymetrics.account.domain.Item;
import org.javamoney.moneta.Money;
import org.jooq.Record;
import org.jooq.RecordMapper;

/**
 * {@link RecordMapper} for {@link Item}.
 */
public class ItemRecordMapper<R extends Record> implements RecordMapper<R, Item> {
    @Override
    public Item map(R record) {
        return Item.builder()
                .id(record.getValue(ITEMS.ID))
                .title(record.getValue(ITEMS.TITLE))
                .moneyAmount(Money.of(record.getValue(ITEMS.MONEY_AMOUNT), record.getValue(ITEMS.CURRENCY_CODE)))
                .period(record.getValue(ITEMS.PERIOD))
                .icon(record.getValue(ITEMS.ICON))
                .type(record.getValue(ITEMS.ITEM_TYPE))
                .build();
    }
}
