package com.github.galleog.piggymetrics.statistics.repository;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

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
    Optional<DataPoint> getByAccountNameAndDate(@NonNull String accountName, @NonNull LocalDate date);

    /**
     * Finds all data points associated with the specified account.
     *
     * @param accountName the account name
     * @return the stream of found data points. Clients should ensure the stream is properly closed
     */
    Stream<DataPoint> listByAccountName(String accountName);

    /**
     * Saves a data point.
     *
     * @param dataPoint the data point to save
     * @return the saved data point
     */
    @NonNull
    DataPoint save(@NonNull DataPoint dataPoint);

    /**
     * Updates a data point.
     *
     * @param dataPoint the data point to update
     * @return the updated data point, or {@link Optional#empty()}
     * if there is no data point with the specified account name and date
     */
    Optional<DataPoint> update(@NonNull DataPoint dataPoint);
}
