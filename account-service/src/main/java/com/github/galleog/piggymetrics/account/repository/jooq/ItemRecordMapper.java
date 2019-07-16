package com.github.galleog.piggymetrics.account.repository.jooq;

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
                .id(record.get(ITEMS.ID))
                .title(record.get(ITEMS.TITLE))
                .moneyAmount(Money.of(record.get(ITEMS.MONEY_AMOUNT), record.get(ITEMS.CURRENCY_CODE)))
                .period(record.get(ITEMS.PERIOD))
                .icon(record.get(ITEMS.ICON))
                .type(record.get(ITEMS.ITEM_TYPE))
                .build();
    }
}
