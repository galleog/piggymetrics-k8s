package com.github.galleog.piggymetrics.statistics.event;

import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
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
import org.javamoney.moneta.Money;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consumer of events on account updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountUpdatedEventConsumer {
    @VisibleForTesting
    static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    private final MonetaryConversionService conversionService;
    private final DataPointRepository dataPointRepository;

    @StreamListener(Sink.INPUT)
    public void updateStatistics(AccountServiceProto.AccountUpdatedEvent event) {
        logger.info("AccountUpdated event for account '{}' received", event.getAccountName());

        List<ItemMetric> metrics = event.getItemsList().stream()
                .map(this::toNormalizedMetric)
                .collect(ImmutableList.toImmutableList());
        BigDecimal saving = conversionService.convert(
                moneyConverter().reverse().convert(event.getSaving().getMoney()), BASE_CURRENCY
        ).getNumber().numberValue(BigDecimal.class);

        doUpdateStatistics(event.getAccountName(), metrics, saving);
    }

    @Transactional
    private void doUpdateStatistics(String accountName, List<ItemMetric> metrics, BigDecimal saving) {
        LocalDate now = LocalDate.now();
        DataPoint dataPoint = DataPoint.builder()
                .accountName(accountName)
                .date(now)
                .build();
        dataPoint.updateStatistics(metrics, saving);
        Optional<DataPoint> updated = dataPointRepository.update(dataPoint);
        if (updated.isPresent()) {
            logger.info("Statistics for the account '{}' updated at {}", accountName, now);
        } else {
            dataPointRepository.save(dataPoint);
            logger.info("Statistics for the account '{}' created at {}", accountName, now);
        }
    }

    private ItemMetric toNormalizedMetric(AccountServiceProto.Item item) {
        Money money = moneyConverter().reverse().convert(item.getMoney());
        BigDecimal amount = conversionService.convert(money, BASE_CURRENCY)
                .divide(TimePeriod.valueOf(item.getPeriod().name()).getBaseRatio())
                .getNumber().numberValue(BigDecimal.class);
        return ItemMetric.builder()
                .type(ItemType.valueOf(item.getType().name()))
                .title(item.getTitle())
                .moneyAmount(amount)
                .build();
    }
}
