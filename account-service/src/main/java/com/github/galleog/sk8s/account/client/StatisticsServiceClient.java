package com.github.galleog.sk8s.account.client;

import com.github.galleog.sk8s.account.domain.AccountBalance;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client for the statistics service.
 */
@FeignClient(name = "statistics-service")
public interface StatisticsServiceClient {
    /**
     * Updates statistics for the specified account.
     *
     * @param accountName the name of the account
     * @param balance     the account balance
     */
    @PutMapping(path = "/statistics/{accountName}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    void updateStatistics(@NonNull @PathVariable("accountName") String accountName, @NonNull AccountBalance balance);
}
