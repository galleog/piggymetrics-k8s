package com.github.galleog.piggymetrics.statistics.repository;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for {@link DataPoint}.
 */
public interface DataPointRepository {
    /**
     * Gets a data point by an account name and date.
     *
     * @param accountName the account name
     * @param date        the data point date
     * @return the found data point, or {@link Optional#empty()}
     * if there is no data point with the specified account name and date
     */
    Mono<DataPoint> getByAccountNameAndDate(@NonNull String accountName, @NonNull LocalDate date);

    /**
     * Finds all data points associated with the specified account.
     *
     * @param accountName the account name
     * @return the stream of found data points. Clients should ensure the stream is properly closed
     */
    Flux<DataPoint> listByAccountName(String accountName);

    /**
     * Saves a data point.
     *
     * @param dataPoint the data point to save
     * @return the saved data point
     */
    Mono<DataPoint> save(@NonNull DataPoint dataPoint);

    /**
     * Updates a data point.
     *
     * @param dataPoint the data point to update
     * @return the updated data point, or {@link Optional#empty()}
     * if there is no data point with the specified account name and date
     */
    Mono<DataPoint> update(@NonNull DataPoint dataPoint);
}
