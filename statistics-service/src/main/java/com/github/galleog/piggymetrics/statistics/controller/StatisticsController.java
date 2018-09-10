package com.github.galleog.piggymetrics.statistics.controller;

import com.github.galleog.piggymetrics.statistics.acl.AccountBalance;
import com.github.galleog.piggymetrics.statistics.acl.ItemConverter;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import com.github.galleog.piggymetrics.statistics.service.StatisticsService;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Stream;

/**
 * REST controller for {@link DataPoint}.
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService;
    private final ItemConverter expenseConverter;
    private final ItemConverter incomeConverter;

    /**
     * Constructs a new instance of the class by its required dependencies.
     */
    public StatisticsController(@NonNull StatisticsService statisticsService, @NonNull ConversionService conversionService) {
        this.statisticsService = statisticsService;
        this.expenseConverter = new ItemConverter(conversionService, ItemType.EXPENSE);
        this.incomeConverter = new ItemConverter(conversionService, ItemType.INCOME);
    }

    /**
     * Gets statistic metrics for the current user.
     *
     * @param principal the current authenticated principal
     * @return a list of data points for the current user
     */
    @GetMapping("/current")
    public List<DataPoint> getCurrentAccountStatistics(@NotNull Principal principal) {
        return statisticsService.findByAccountName(principal.getName());
    }

    /**
     * Gets statistic metric for the specified account.
     *
     * @param accountName the name of the account to get statistics for
     * @return a list of data points for the specified account
     */
    @PreAuthorize("#oauth2.hasScope('server') or #accountName.equals('demo')")
    @GetMapping("/{accountName}")
    public List<DataPoint> getStatisticsByAccountName(@PathVariable @NotBlank String accountName) {
        return statisticsService.findByAccountName(accountName);
    }

    /**
     * Updates statistic metrics for the specified account.
     *
     * @param accountName the name of the account to be updated
     * @param balance     the balance of the account
     * @return the created or updated data point
     */
    @PreAuthorize("#oauth2.hasScope('server')")
    @PutMapping(path = "/{accountName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DataPoint updateAccountStatistics(@PathVariable @NotBlank String accountName,
                                             @NotNull @RequestBody AccountBalance balance) {
        List<ItemMetric> metrics = Stream.concat(
                balance.getIncomes().stream().map(incomeConverter::convert),
                balance.getExpenses().stream().map(expenseConverter::convert)
        ).collect(ImmutableList.toImmutableList());
        return statisticsService.save(accountName, metrics, balance.getSaving().getMoneyAmount());
    }

    /**
     * Returns {@link HttpStatus#BAD_REQUEST} when an exception of type {@link NullPointerException},
     * {@link IllegalArgumentException} or {@link IllegalStateException} is thrown.
     *
     * @param e the thrown exception
     */
    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void processValidationError(Exception e) {
        logger.error("HTTP 400 Bad Request is returned because invalid data are specified", e);
    }
}
