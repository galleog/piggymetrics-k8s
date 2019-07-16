package com.github.galleog.piggymetrics.statistics.domain;

import static com.github.galleog.piggymetrics.statistics.domain.ItemType.EXPENSE;
import static com.github.galleog.piggymetrics.statistics.domain.ItemType.INCOME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Entity for daily time series data points containing the current account state.
 */
@Getter
public class DataPoint {
    /**
     * Account name this data point is associated with.
     */
    @NonNull
    private String accountName;

    /**
     * Date of this data point.
     */
    @NonNull
    private LocalDate date;

    /**
     * Account incomes and expenses.
     */
    @NonNull
    private List<ItemMetric> metrics;

    /**
     * Total statistics of incomes, expenses, and savings.
     */
    @NonNull
    private Map<StatisticalMetric, BigDecimal> statistics;

    @Builder
    private DataPoint(@NonNull String accountName, @NonNull LocalDate date, @NonNull @Singular Collection<ItemMetric> metrics,
                      @NonNull @Singular Map<StatisticalMetric, BigDecimal> statistics) {
        setAccountName(accountName);
        setDate(date);
        setMetrics(metrics);
        setStatistics(statistics);
    }

    /**
     * Updates this data points.
     *
     * @param metrics the new data point metrics
     * @param saving  the new saving
     * @throws NullPointerException if the metrics themselves, any metric they contain or the saving are {@code null}
     */
    public void updateStatistics(@NonNull Collection<ItemMetric> metrics, @NonNull BigDecimal saving) {
        Validate.notNull(saving);
        Validate.isTrue(saving.signum() != -1);

        setMetrics(metrics);
        setStatistics(
                ImmutableMap.of(
                        StatisticalMetric.INCOMES_AMOUNT,
                        metrics.stream()
                                .filter(metric -> INCOME.equals(metric.getType()))
                                .map(ItemMetric::getMoneyAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        StatisticalMetric.EXPENSES_AMOUNT,
                        metrics.stream()
                                .filter(metric -> EXPENSE.equals(metric.getType()))
                                .map(ItemMetric::getMoneyAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        StatisticalMetric.SAVING_AMOUNT,
                        saving
                )
        );
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("accountName", getAccountName())
                .append("date", DateTimeFormatter.ISO_DATE.format(getDate()))
                .build();
    }

    private void setAccountName(String accountName) {
        Validate.notBlank(accountName);
        this.accountName = accountName;
    }

    private void setDate(LocalDate date) {
        Validate.notNull(date);
        this.date = date;
    }

    private void setMetrics(Collection<ItemMetric> metrics) {
        Validate.noNullElements(metrics);
        this.metrics = ImmutableList.copyOf(metrics);
    }

    private void setStatistics(Map<StatisticalMetric, BigDecimal> statistics) {
        Validate.notNull(statistics);
        Validate.isTrue(statistics.values()
                .stream()
                .allMatch(amount -> amount.signum() >= 0));
        this.statistics = ImmutableMap.copyOf(statistics);
    }
}
