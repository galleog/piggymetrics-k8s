package com.github.galleog.piggymetrics.statistics.domain;

import static com.github.galleog.piggymetrics.statistics.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.INCOME;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Columns;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entity for daily time series data points containing the current account state.
 */
@Entity
@Table(name = DataPoint.TABLE_NAME)
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Configurable(preConstruction = true, dependencyCheck = true)
public class DataPoint implements Serializable {
    @VisibleForTesting
    public static final String TABLE_NAME = "data_points";
    @VisibleForTesting
    public static final String STATISTICS_TABLE_NAME = "statistic_metrics";

    /**
     * Base currency.
     */
    public static final CurrencyUnit BASE_CURRENCY = Monetary.getCurrency("USD");

    @Autowired
    private transient ConversionService conversionService;

    @EmbeddedId
    @Getter(AccessLevel.PRIVATE)
    @AttributeOverride(name = "date", column = @Column(name = "data_point_date"))
    private DataPointId id;

    /**
     * Account incomes and expenses.
     */
    @Getter(AccessLevel.PRIVATE)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dataPoint")
    private Set<ItemMetric> metrics = new HashSet<>();

    /**
     * Total statistics of incomes, expenses, and savings.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyEnumerated(EnumType.ORDINAL)
    @MapKeyColumn(name = "statistic_metric")
    @CollectionTable(name = STATISTICS_TABLE_NAME, joinColumns = {
            @JoinColumn(name = "account"), @JoinColumn(name = "data_point_date")
    })
    @Columns(columns = {@Column(name = "currency_code"), @Column(name = "amount")})
    private Map<StatisticMetric, Money> statistics = new HashMap<>();

    @Version
    @SuppressWarnings("unused")
    private Integer version;

    @Builder
    @SuppressWarnings("unused")
    private DataPoint(@NonNull String account, @Nullable LocalDate date, @NonNull @Singular Collection<ItemMetric> metrics,
                      @NonNull Money saving) {
        setId(DataPointId.of(account, Optional.ofNullable(date).orElse(LocalDate.now())));
        setMetrics(metrics);
        setStatistics(saving);
    }

    /**
     * Updates this data points.
     *
     * @param metrics the new data point metrics
     * @param saving  the new saving
     * @throws NullPointerException if the metrics themselves, any metric they contain or the saving are {@code null}
     */
    public void update(@NonNull Collection<ItemMetric> metrics, @NonNull Money saving) {
        setMetrics(metrics);
        setStatistics(saving);
    }

    /**
     * Gets the account this data point is associated with.
     */
    @NonNull
    public String getAccount() {
        return getId().getAccount();
    }

    /**
     * Gets the data of this data point.
     */
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate getDate() {
        return getId().getDate();
    }

    /**
     * Gets the incomes of this data point.
     */
    @NonNull
    public Set<ItemMetric> getIncomes() {
        return getMetrics().stream().filter(metric -> INCOME.equals(metric.getType()))
                .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Gets the expenses of this data point.
     */
    @NonNull
    public Set<ItemMetric> getExpenses() {
        return getMetrics().stream().filter(metric -> EXPENSE.equals(metric.getType()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private void setId(DataPointId id) {
        this.id = id;
    }

    private void setMetrics(Collection<ItemMetric> metrics) {
        Validate.notNull(metrics);
        this.metrics.clear();
        metrics.forEach(this::addMetric);
    }

    /**
     * Gets the total statistics of incomes, expenses and savings.
     */
    @NonNull
    public Map<StatisticMetric, Money> getStatistics() {
        return ImmutableMap.copyOf(this.statistics);
    }

    private void setStatistics(Money saving) {
        Validate.notNull(saving);

        this.statistics.put(StatisticMetric.INCOMES_AMOUNT, getIncomes().stream()
                .map(ItemMetric::getMoneyAmount)
                .reduce(Money.of(0, BASE_CURRENCY), Money::add));
        this.statistics.put(StatisticMetric.EXPENSES_AMOUNT, getExpenses().stream()
                .map(ItemMetric::getMoneyAmount)
                .reduce(Money.of(0, BASE_CURRENCY), Money::add));
        this.statistics.put(StatisticMetric.SAVING_AMOUNT, conversionService.convert(saving, BASE_CURRENCY));
    }

    private void addMetric(ItemMetric metric) {
        Validate.notNull(metric);
        metric.setDataPoint(this);
        getMetrics().add(metric);
    }
}
