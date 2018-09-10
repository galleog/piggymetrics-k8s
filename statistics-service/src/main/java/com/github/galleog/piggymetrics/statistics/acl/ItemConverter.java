package com.github.galleog.piggymetrics.statistics.acl;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.BASE_CURRENCY;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import com.google.common.base.Converter;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;

/**
 * {@link Converter} that converts {@link Item} to {@link ItemMetric}.
 */
public class ItemConverter extends Converter<Item, ItemMetric> {
    private ConversionService conversionService;
    private ItemType type;

    /**
     * Constructs a new converter.
     *
     * @param type              the type of converted items
     * @param conversionService the service to convert the item {@link Money} amount to
     *                          the {@link DataPoint#BASE_CURRENCY base currency}
     * @throws NullPointerException if the convertion service or type is {@code null}
     */
    public ItemConverter(@NonNull ConversionService conversionService, @NonNull ItemType type) {
        setConversionService(conversionService);
        setType(type);
    }

    @Override
    protected ItemMetric doForward(Item item) {
        return ItemMetric.builder()
                .type(type)
                .title(item.getTitle())
                .moneyAmount(getNormalizedAmount(item))
                .build();
    }

    @Override
    protected Item doBackward(ItemMetric itemMetric) {
        throw new UnsupportedOperationException();
    }

    private void setConversionService(ConversionService conversionService) {
        Validate.notNull(conversionService);
        this.conversionService = conversionService;
    }

    private void setType(ItemType type) {
        Validate.notNull(type);
        this.type = type;
    }

    private Money getNormalizedAmount(Item item) {
        return conversionService.convert(item.getMoneyAmount(), BASE_CURRENCY)
                .divide(item.getPeriod().getBaseRatio());
    }
}
