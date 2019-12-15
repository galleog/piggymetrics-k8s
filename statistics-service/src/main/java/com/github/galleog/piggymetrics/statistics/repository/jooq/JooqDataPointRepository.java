package com.github.galleog.piggymetrics.statistics.repository.jooq;

import static com.github.galleog.piggymetrics.statistics.domain.Sequences.ITEM_METRIC_SEQ;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.DATA_POINTS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.ITEM_METRICS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.STATISTICAL_METRICS;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.DataPointsRecord;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.ItemMetricsRecord;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.StatisticalMetricsRecord;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of {@link DataPointRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqDataPointRepository implements DataPointRepository {
    private final DSLContext dsl;

    private static DataPoint toDataPoint(List<Record> records) {
        Record record = records.get(0);

        List<ItemMetric> itemMetrics = records.stream()
                .filter(r -> r.get(ITEM_METRICS.ID) != null)
                .map(r -> r.into(ITEM_METRICS))
                .distinct()
                .map(r -> r.into(ItemMetric.class))
                .collect(ImmutableList.toImmutableList());
        Map<StatisticalMetric, BigDecimal> statistics = records.stream()
                .map(r -> r.into(STATISTICAL_METRICS))
                .distinct()
                .collect(ImmutableMap.toImmutableMap(
                        StatisticalMetricsRecord::getStatisticalMetric,
                        StatisticalMetricsRecord::getMoneyAmount
                ));
        return DataPoint.builder()
                .accountName(record.get(DATA_POINTS.ACCOUNT_NAME))
                .date(record.get(DATA_POINTS.DATA_POINT_DATE))
                .metrics(itemMetrics)
                .statistics(statistics)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataPoint> getByAccountNameAndDate(@NonNull String accountName, @NonNull LocalDate date) {
        Validate.notNull(accountName);
        Validate.notNull(date);

        Result<Record> records = dsl.select()
                .from(DATA_POINTS)
                .leftJoin(ITEM_METRICS).on(ITEM_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                        .and(ITEM_METRICS.DATA_POINT_DATE.endsWith(DATA_POINTS.DATA_POINT_DATE)))
                .join(STATISTICAL_METRICS).on(STATISTICAL_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                        .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                .where(DATA_POINTS.ACCOUNT_NAME.eq(accountName).and(DATA_POINTS.DATA_POINT_DATE.eq(date)))
                .fetch();
        return records.isEmpty() ? Optional.empty() : Optional.of(toDataPoint(records));
    }

    @Override
    public Stream<DataPoint> listByAccountName(@NonNull String accountName) {
        Validate.notNull(accountName);

        Cursor<Record> cursor = dsl.select()
                .from(DATA_POINTS)
                .leftJoin(ITEM_METRICS).on(ITEM_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                        .and(ITEM_METRICS.DATA_POINT_DATE.endsWith(DATA_POINTS.DATA_POINT_DATE)))
                .join(STATISTICAL_METRICS).on(STATISTICAL_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                        .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                .where(DATA_POINTS.ACCOUNT_NAME.eq(accountName))
                .orderBy(DATA_POINTS.DATA_POINT_DATE)
                .fetchLazy();
        Spliterator<DataPoint> spliterator = Spliterators.spliteratorUnknownSize(
                new DataPointIterator(cursor),
                Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.IMMUTABLE
        );
        return StreamSupport.stream(spliterator,false)
                .onClose(cursor::close);
    }

    @Override
    @NonNull
    @Transactional
    public DataPoint save(@NonNull DataPoint dataPoint) {
        Validate.notNull(dataPoint);

        DataPointsRecord record = dsl.newRecord(DATA_POINTS);
        record.setAccountName(dataPoint.getAccountName());
        record.setDataPointDate(dataPoint.getDate());
        record.insert();
        return insertRecords(record, dataPoint);
    }

    @Override
    @Transactional
    public Optional<DataPoint> update(@NonNull DataPoint dataPoint) {
        Validate.notNull(dataPoint);

        DataPointsRecord record = dsl.selectFrom(DATA_POINTS)
                .where(DATA_POINTS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                        .and(DATA_POINTS.DATA_POINT_DATE.eq(dataPoint.getDate())))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }

        dsl.deleteFrom(ITEM_METRICS)
                .where(ITEM_METRICS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                        .and(ITEM_METRICS.DATA_POINT_DATE.eq(dataPoint.getDate())))
                .execute();
        dsl.deleteFrom(STATISTICAL_METRICS)
                .where(STATISTICAL_METRICS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                        .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(dataPoint.getDate())))
                .execute();

        return Optional.of(insertRecords(record, dataPoint));
    }

    private List<ItemMetricsRecord> insertItems(DataPoint dataPoint) {
        if (dataPoint.getMetrics().isEmpty()) {
            return ImmutableList.of();
        }

        List<ItemMetricsRecord> records = dataPoint.getMetrics().stream()
                .map(metric -> {
                    ItemMetricsRecord record = dsl.newRecord(ITEM_METRICS);
                    record.from(metric);
                    record.setId(dsl.nextval(ITEM_METRIC_SEQ));
                    record.setAccountName(dataPoint.getAccountName());
                    record.setDataPointDate(dataPoint.getDate());
                    record.setItemType(metric.getType());
                    return record;
                }).collect(ImmutableList.toImmutableList());
        dsl.batchInsert(records).execute();
        return records;
    }

    private List<StatisticalMetricsRecord> insertStatistics(DataPoint dataPoint) {
        return dataPoint.getStatistics().entrySet()
                .stream()
                .map(entry -> {
                    StatisticalMetricsRecord record = dsl.newRecord(STATISTICAL_METRICS);
                    record.setAccountName(dataPoint.getAccountName());
                    record.setDataPointDate(dataPoint.getDate());
                    record.setStatisticalMetric(entry.getKey());
                    record.setMoneyAmount(entry.getValue());
                    record.insert();
                    return record;
                }).collect(ImmutableList.toImmutableList());
    }

    private DataPoint insertRecords(DataPointsRecord dataPointsRecord, DataPoint dataPoint) {
        List<ItemMetricsRecord> itemMetricsRecords = insertItems(dataPoint);
        List<ItemMetric> metrics = itemMetricsRecords.stream()
                .map(rec -> rec.into(ItemMetric.class))
                .collect(ImmutableList.toImmutableList());

        List<StatisticalMetricsRecord> statisticalMetricsRecords = insertStatistics(dataPoint);
        Map<StatisticalMetric, BigDecimal> statistics = statisticalMetricsRecords.stream()
                .collect(ImmutableMap.toImmutableMap(
                        StatisticalMetricsRecord::getStatisticalMetric,
                        StatisticalMetricsRecord::getMoneyAmount
                ));

        return DataPoint.builder()
                .accountName(dataPointsRecord.getAccountName())
                .date(dataPointsRecord.getDataPointDate())
                .metrics(metrics)
                .statistics(statistics)
                .build();
    }

    @RequiredArgsConstructor
    private static class DataPointIterator implements Iterator<DataPoint> {
        private final Cursor<Record> cursor;

        private List<Record> records;
        private LocalDate date;

        @Override
        public boolean hasNext() {
            return (date == null && cursor.hasNext()) || !CollectionUtils.isEmpty(records);
        }

        @Override
        public DataPoint next() {
            if (!hasNext()) {
                throw new NoSuchElementException("There are no more records to fetch from this Cursor");
            }

            while (cursor.hasNext()) {
                Record record = cursor.fetchNext();
                if (date == null) {
                    date = record.get(DATA_POINTS.DATA_POINT_DATE);
                    records = new ArrayList<>();
                    records.add(record);
                } else if (!date.equals(record.get(DATA_POINTS.DATA_POINT_DATE))) {
                    // return a previously collected data point
                    DataPoint next = toDataPoint(records);

                    // start collecting records for a new date
                    date = record.get(DATA_POINTS.DATA_POINT_DATE);
                    records.clear();
                    records.add(record);

                    return next;
                } else {
                    records.add(record);
                }
            }

            DataPoint next = toDataPoint(records);
            records = null;
            return next;
        }
    }
}
