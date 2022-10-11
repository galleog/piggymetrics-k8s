package com.github.galleog.piggymetrics.statistics.repository.jooq;

import static com.github.galleog.piggymetrics.statistics.domain.Sequences.ITEM_METRIC_SEQ;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.DATA_POINTS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.ITEM_METRICS;
import static com.github.galleog.piggymetrics.statistics.domain.Tables.STATISTICAL_METRICS;
import static org.jooq.impl.DSL.val;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.DataPointsRecord;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.ItemMetricsRecord;
import com.github.galleog.piggymetrics.statistics.domain.tables.records.StatisticalMetricsRecord;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.piggymetrics.autoconfigure.jooq.TransactionAwareJooqWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link DataPointRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqDataPointRepository implements DataPointRepository {
    private final TransactionAwareJooqWrapper wrapper;

    @Override
    @Transactional(readOnly = true)
    public Mono<DataPoint> getByAccountNameAndDate(@NonNull String accountName, @NonNull LocalDate date) {
        Validate.notNull(accountName);
        Validate.notNull(date);
        return wrapper.withDSLContextMany(ctx ->
                        ctx.select()
                                .from(DATA_POINTS)
                                .leftJoin(ITEM_METRICS).on(ITEM_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                                        .and(ITEM_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                                .join(STATISTICAL_METRICS).on(STATISTICAL_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                                        .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                                .where(DATA_POINTS.ACCOUNT_NAME.eq(accountName).and(DATA_POINTS.DATA_POINT_DATE.eq(date)))
                ).collectList()
                .mapNotNull(this::toDataPoint);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<DataPoint> listByAccountName(@NonNull String accountName) {
        Validate.notNull(accountName);
        return wrapper.withDSLContextMany(ctx ->
                        ctx.select()
                                .from(DATA_POINTS)
                                .leftJoin(ITEM_METRICS).on(ITEM_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                                        .and(ITEM_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                                .join(STATISTICAL_METRICS).on(STATISTICAL_METRICS.ACCOUNT_NAME.eq(DATA_POINTS.ACCOUNT_NAME)
                                        .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(DATA_POINTS.DATA_POINT_DATE)))
                                .where(DATA_POINTS.ACCOUNT_NAME.eq(accountName))
                ).bufferUntilChanged(record -> record.get(DATA_POINTS.DATA_POINT_DATE))
                .map(this::toDataPoint);
    }

    @Override
    @Transactional
    public Mono<DataPoint> save(@NonNull DataPoint dataPoint) {
        Validate.notNull(dataPoint);
        return insertDataPointSql(dataPoint)
                .map(record ->
                        DataPoint.builder()
                                .accountName(record.getAccountName())
                                .date(record.getDataPointDate())
                ).flatMap(builder ->
                        insertItemMetrics(dataPoint)
                                .map(builder::metrics)
                ).flatMap(builder ->
                        insertStatistics(dataPoint)
                                .map(statistics -> builder.statistics(statistics).build())
                );
    }

    @Override
    @Transactional
    public Mono<DataPoint> update(@NonNull DataPoint dataPoint) {
        Validate.notNull(dataPoint);
        return wrapper.withDSLContext(ctx ->
                ctx.select()
                        .from(DATA_POINTS)
                        .where(DATA_POINTS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                                .and(DATA_POINTS.DATA_POINT_DATE.eq(dataPoint.getDate())))
        ).map(record ->
                DataPoint.builder()
                        .accountName(record.get(DATA_POINTS.ACCOUNT_NAME))
                        .date(record.get(DATA_POINTS.DATA_POINT_DATE))
        ).flatMap(builder ->
                deleteItemMetricsSql(dataPoint)
                        .map(i -> builder)
        ).flatMap(builder ->
                insertItemMetrics(dataPoint)
                        .map(builder::metrics)
        ).flatMap(builder ->
                deleteStatisticsSql(dataPoint)
                        .map(i -> builder)
        ).flatMap(builder ->
                insertStatistics(dataPoint)
                        .map(statistics -> builder.statistics(statistics).build())
        );
    }

    private Mono<DataPointsRecord> insertDataPointSql(DataPoint dataPoint) {
        return wrapper.withDSLContext(ctx ->
                ctx.insertInto(DATA_POINTS)
                        .columns(DATA_POINTS.ACCOUNT_NAME, DATA_POINTS.DATA_POINT_DATE)
                        .values(dataPoint.getAccountName(), dataPoint.getDate())
                        .returning()
        );
    }

    private Mono<List<ItemMetric>> insertItemMetrics(DataPoint dataPoint) {
        return Flux.fromIterable(dataPoint.getMetrics())
                .flatMap(itemMetric -> wrapper.withDSLContext(ctx ->
                        ctx.insertInto(ITEM_METRICS)
                                .columns(
                                        ITEM_METRICS.ID,
                                        ITEM_METRICS.ACCOUNT_NAME,
                                        ITEM_METRICS.DATA_POINT_DATE,
                                        ITEM_METRICS.TITLE,
                                        ITEM_METRICS.MONEY_AMOUNT,
                                        ITEM_METRICS.ITEM_TYPE
                                ).values(
                                        ITEM_METRIC_SEQ.nextval(),
                                        val(dataPoint.getAccountName()),
                                        val(dataPoint.getDate()),
                                        val(itemMetric.getTitle()),
                                        val(itemMetric.getMoneyAmount()),
                                        val(itemMetric.getType())
                                ).returning()
                )).map(this::toItemMetric)
                .collectList();
    }

    private Mono<Integer> deleteItemMetricsSql(DataPoint dataPoint) {
        return wrapper.withDSLContext(ctx ->
                ctx.deleteFrom(ITEM_METRICS)
                        .where(ITEM_METRICS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                                .and(ITEM_METRICS.DATA_POINT_DATE.eq(dataPoint.getDate())))
        );
    }

    private Mono<Map<StatisticalMetric, BigDecimal>> insertStatistics(DataPoint dataPoint) {
        return Flux.fromIterable(dataPoint.getStatistics().entrySet())
                .flatMap(entry -> wrapper.withDSLContext(ctx ->
                        ctx.insertInto(STATISTICAL_METRICS)
                                .columns(
                                        STATISTICAL_METRICS.ACCOUNT_NAME,
                                        STATISTICAL_METRICS.DATA_POINT_DATE,
                                        STATISTICAL_METRICS.STATISTICAL_METRIC,
                                        STATISTICAL_METRICS.MONEY_AMOUNT
                                ).values(
                                        dataPoint.getAccountName(),
                                        dataPoint.getDate(),
                                        entry.getKey(),
                                        entry.getValue()
                                ).returning()
                )).map(record ->
                        Maps.immutableEntry(
                                record.getStatisticalMetric(),
                                record.getMoneyAmount()
                        )
                ).collectList()
                .map(ImmutableMap::copyOf);
    }

    private Mono<Integer> deleteStatisticsSql(DataPoint dataPoint) {
        return wrapper.withDSLContext(ctx ->
                ctx.deleteFrom(STATISTICAL_METRICS)
                        .where(STATISTICAL_METRICS.ACCOUNT_NAME.eq(dataPoint.getAccountName())
                                .and(STATISTICAL_METRICS.DATA_POINT_DATE.eq(dataPoint.getDate())))
        );
    }

    private DataPoint toDataPoint(List<Record> records) {
        if (records.isEmpty()) {
            return null;
        }

        var itemMetrics = records.stream()
                .filter(r -> r.get(ITEM_METRICS.ID) != null)
                .map(r -> r.into(ITEM_METRICS))
                .distinct()
                .map(this::toItemMetric)
                .collect(ImmutableList.toImmutableList());
        var statistics = records.stream()
                .map(r -> r.into(STATISTICAL_METRICS))
                .distinct()
                .collect(ImmutableMap.toImmutableMap(
                        StatisticalMetricsRecord::getStatisticalMetric,
                        StatisticalMetricsRecord::getMoneyAmount
                ));

        Record record = records.get(0);
        return DataPoint.builder()
                .accountName(record.get(DATA_POINTS.ACCOUNT_NAME))
                .date(record.get(DATA_POINTS.DATA_POINT_DATE))
                .metrics(itemMetrics)
                .statistics(statistics)
                .build();
    }

    private ItemMetric toItemMetric(ItemMetricsRecord record) {
        return ItemMetric.builder()
                .id(record.getId())
                .type(record.getItemType())
                .title(record.getTitle())
                .moneyAmount(record.getMoneyAmount())
                .build();
    }
}
