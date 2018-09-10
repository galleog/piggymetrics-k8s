package com.github.galleog.piggymetrics.statistics.service;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.DataPointId;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.javamoney.moneta.Money;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service to get account statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final DataPointRepository dataPointRepository;

    /**
     * Finds data points associated with the specified account.
     *
     * @param account the account name
     * @return a list of the found data points
     * @throws NullPointerException     if the account name is {@code null}
     * @throws IllegalArgumentException if the account name is blank
     */
    @NonNull
    public List<DataPoint> findByAccountName(@NonNull String account) {
        Validate.notBlank(account);
        return dataPointRepository.findByIdAccount(account);
    }

    /**
     * Creates a new data point of rewrites the existing one within a day.
     *
     * @param accountName the name of the account the metrics are for
     * @param metrics     the account metrics
     * @param saving      the account saving
     * @throws NullPointerException     if the account name, metrics or saving are {@code null}
     * @throws IllegalArgumentException if the account name is blank or any metric within {@code metrics} is {@code null}
     */
    @NonNull
    @Transactional
    public DataPoint save(@NonNull String accountName, @NonNull Collection<ItemMetric> metrics, @NonNull Money saving) {
        Validate.notBlank(accountName);
        Validate.noNullElements(metrics);
        Validate.notNull(saving);

        LocalDate now = LocalDate.now();

        DataPoint dataPoint;
        Optional<DataPoint> optional = dataPointRepository.findById(DataPointId.of(accountName, now));
        if (optional.isPresent()) {
            logger.debug("Existing data point for the account {} at {} will be rewritten", accountName, now);

            dataPoint = optional.get();
            dataPoint.update(metrics, saving);
        } else {
            dataPoint = DataPoint.builder()
                    .account(accountName)
                    .date(now)
                    .metrics(metrics)
                    .saving(saving)
                    .build();
        }
        dataPointRepository.save(dataPoint);

        logger.debug("Data point for the account {} created at {}", accountName, now);
        return dataPoint;
    }
}
