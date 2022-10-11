package com.github.galleog.piggymetrics.statistics.event;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.updateStatistics;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto.AccountUpdatedEvent;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.TimePeriod;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import com.github.galleog.piggymetrics.statistics.service.MonetaryConversionService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Consumer of events on account updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountUpdatedEventConsumer implements Function<Flux<ConsumerRecord<String, AccountUpdatedEvent>>, Mono<Void>> {
    @VisibleForTesting
    static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    private final MonetaryConversionService conversionService;
    private final DataPointRepository dataPointRepository;
    private final TransactionalOperator operator;

    @Override
    public Mono<Void> apply(Flux<ConsumerRecord<String, AccountUpdatedEvent>> records) {
        return records.map(ConsumerRecord::value)
                .doOnNext(event -> logger.info("AccountUpdatedEvent for account '{}' received", event.getAccountName()))
                .flatMap(this::doUpdateStatistics)
                .then();
    }

    private Mono<DataPoint> doUpdateStatistics(AccountUpdatedEvent event) {
        var metrics = event.getItemsList()
                .stream()
                .map(this::toNormalizedMetric)
                .collect(ImmutableList.toImmutableList());
        var saving = conversionService.convert(
                moneyConverter().reverse().convert(event.getSaving().getMoney()), BASE_CURRENCY
        ).getNumber().numberValue(BigDecimal.class);
        var dataPoint = updateStatistics(event.getAccountName(), metrics, saving);

        return dataPointRepository.update(dataPoint)
                .doOnNext(dp -> logger.info("Statistics for the account '{}' updated at {}", dp.getAccountName(), dp.getDate()))
                .switchIfEmpty(Mono.defer(() -> dataPointRepository.save(dataPoint)
                        .doOnNext(dp ->
                                logger.info("Statistics for the account '{}' created at {}", dp.getAccountName(), dp.getDate()))
                )).as(operator::transactional);
    }

    private ItemMetric toNormalizedMetric(AccountServiceProto.Item item) {
        var money = moneyConverter().reverse().convert(item.getMoney());
        var amount = conversionService.convert(money, BASE_CURRENCY)
                .divide(TimePeriod.valueOf(item.getPeriod().name()).getBaseRatio())
                .getNumber().numberValue(BigDecimal.class);
        return ItemMetric.builder()
                .type(ItemType.valueOf(item.getType().name()))
                .title(item.getTitle())
                .moneyAmount(amount)
                .build();
    }
}
