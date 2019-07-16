package com.github.galleog.piggymetrics.statistics.repository.jooq;

import static com.github.galleog.piggymetrics.statistics.domain.Tables.ITEM_METRICS;

import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import org.jooq.Record;
import org.jooq.RecordMapper;

/**
 * {@link RecordMapper} for {@link ItemMetric}.
 */
public class ItemMetricRecordMapper<R extends Record> implements RecordMapper<R, ItemMetric> {
    @Override
    public ItemMetric map(R record) {
        return ItemMetric.builder()
                .id(record.get(ITEM_METRICS.ID))
                .type(record.get(ITEM_METRICS.ITEM_TYPE))
                .title(record.get(ITEM_METRICS.TITLE))
                .moneyAmount(record.get(ITEM_METRICS.MONEY_AMOUNT))
                .build();
    }
}
