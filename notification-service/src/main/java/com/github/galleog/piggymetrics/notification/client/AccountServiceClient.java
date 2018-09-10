package com.github.galleog.piggymetrics.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for the account service.
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient {
    /**
     * Gets an account by its name.
     *
     * @param accountName the name of the account
     * @return a JSON string containing the account data
     */
    @GetMapping(path = "/accounts/{accountName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    String getAccount(@PathVariable("accountName") String accountName);
}
