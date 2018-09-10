package com.github.galleog.piggymetrics.statistics.repository;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.DataPointId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link DataPoint}.
 */
public interface DataPointRepository extends CrudRepository<DataPoint, DataPointId> {
    /**
     * Find a data point by its identifier.
     *
     * @param id the data point identifier
     * @return the found data point, or {@link Optional#empty()} if there is no data point with the given identifier
     */
    Optional<DataPoint> findById(DataPointId id);

    /**
     * Finds all data points associated with the specified account.
     *
     * @param account the account name
     * @return a list of the found data points
     */
    List<DataPoint> findByIdAccount(String account);
}
