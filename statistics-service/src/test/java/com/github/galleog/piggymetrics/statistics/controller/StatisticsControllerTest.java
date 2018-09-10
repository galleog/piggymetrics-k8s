package com.github.galleog.piggymetrics.statistics.controller;

import static com.github.galleog.piggymetrics.statistics.domain.DataPoint.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.piggymetrics.statistics.acl.AccountBalance;
import com.github.galleog.piggymetrics.statistics.acl.Item;
import com.github.galleog.piggymetrics.statistics.acl.Saving;
import com.github.galleog.piggymetrics.statistics.acl.TimePeriod;
import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.domain.ItemType;
import com.github.galleog.piggymetrics.statistics.domain.StatisticMetric;
import com.github.galleog.piggymetrics.statistics.service.ConversionService;
import com.github.galleog.piggymetrics.statistics.service.StatisticsService;
import com.google.common.collect.ImmutableList;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tests for {@link StatisticsController}.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@WebMvcTest(StatisticsController.class)
@WithMockUser(StatisticsControllerTest.ACCOUNT_NAME)
class StatisticsControllerTest {
    static final String ACCOUNT_NAME = "test";
    private static final String BASE_URL = "/statistics";
    private static final LocalDate NOW = LocalDate.now();
    private static final String GROCERY = "Grocery";
    private static final double GROCERY_AMOUNT = 10;
    private static final Money GROCERY_MONEY = Money.of(GROCERY_AMOUNT, BASE_CURRENCY);
    private static final String SALARY = "Salary";
    private static final double SALARY_AMOUNT = 300;
    private static final Money SALARY_MONEY = Money.of(SALARY_AMOUNT, BASE_CURRENCY);
    private static final double SAVING_AMOUNT = 5900;
    private static final Money SAVING_MONEY = Money.of(SAVING_AMOUNT, BASE_CURRENCY);

    @MockBean
    private StatisticsService statisticsService;
    @MockBean
    private ConversionService conversionService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(conversionService.convert(any(Money.class), eq(BASE_CURRENCY)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test for {@link StatisticsController#getStatisticsByAccountName(String)}.
     */
    @Test
    void shouldGetStatisticsByAccountName() throws Exception {
        DataPoint dataPoint = stubDataPoint();
        when(statisticsService.findByAccountName(ACCOUNT_NAME)).thenReturn(ImmutableList.of(dataPoint));

        mockMvc.perform(get(BASE_URL + "/" + ACCOUNT_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].account").value(ACCOUNT_NAME))
                .andExpect(jsonPath("$[0].date").value(NOW.format(DateTimeFormatter.ISO_DATE)))
                .andExpect(jsonPath("$[0].expenses").value(hasSize(1)))
                .andExpect(jsonPath("$[0].expenses[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].expenses[0].type").doesNotExist())
                .andExpect(jsonPath("$[0].expenses[0].title").value(GROCERY))
                .andExpect(jsonPath("$[0].expenses[0].moneyAmount.amount").value(GROCERY_AMOUNT))
                .andExpect(jsonPath("$[0].expenses[0].moneyAmount.currency").value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$[0].incomes").value(hasSize(1)))
                .andExpect(jsonPath("$[0].incomes[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].incomes[0].type").doesNotExist())
                .andExpect(jsonPath("$[0].incomes[0].title").value(SALARY))
                .andExpect(jsonPath("$[0].incomes[0].moneyAmount.amount").value(SALARY_AMOUNT))
                .andExpect(jsonPath("$[0].incomes[0].moneyAmount.currency").value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.INCOMES_AMOUNT.name() + ".amount")
                        .value(SALARY_AMOUNT))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.INCOMES_AMOUNT.name() + ".currency")
                        .value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.EXPENSES_AMOUNT.name() + ".amount")
                        .value(GROCERY_AMOUNT))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.EXPENSES_AMOUNT.name() + ".currency")
                        .value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.SAVING_AMOUNT.name() + ".amount")
                        .value(SAVING_AMOUNT))
                .andExpect(jsonPath("$[0].statistics." + StatisticMetric.SAVING_AMOUNT.name() + ".currency")
                        .value(BASE_CURRENCY.getCurrencyCode()));
    }

    /**
     * Test for {@link StatisticsController#getCurrentAccountStatistics(Principal)}.
     */
    @Test
    void shouldGetCurrentAccountStatistics() throws Exception {
        when(statisticsService.findByAccountName(ACCOUNT_NAME)).thenReturn(ImmutableList.of());

        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(empty()));
    }

    /**
     * Test for {@link StatisticsController#updateAccountStatistics(String, AccountBalance)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateAccountStatistics() throws Exception {
        ArgumentCaptor<List<ItemMetric>> metricsCaptor = ArgumentCaptor.forClass(List.class);
        doReturn(stubDataPoint()).when(statisticsService).save(eq(ACCOUNT_NAME), metricsCaptor.capture(), eq(SAVING_MONEY));

        mockMvc.perform(put(BASE_URL + "/" + ACCOUNT_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeAccountBalsnceJson()))
                .andExpect(status().isOk());

        assertThat(metricsCaptor.getValue()).extracting(ItemMetric::getType, ItemMetric::getTitle, ItemMetric::getMoneyAmount)
                .containsExactlyInAnyOrder(
                        tuple(ItemType.EXPENSE, GROCERY, GROCERY_MONEY),
                        tuple(ItemType.INCOME, SALARY, SALARY_MONEY)
                );
    }

    /**
     * Test for {@link StatisticsController#processValidationError(Exception)}.
     */
    @Test
    void shouldReturnHTTP400BadRequest() throws Exception {
        when(statisticsService.save(eq(ACCOUNT_NAME), anyList(), eq(SAVING_MONEY))).thenThrow(IllegalArgumentException.class);
        mockMvc.perform(put(BASE_URL + "/" + ACCOUNT_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeAccountBalsnceJson()))
                .andExpect(status().isBadRequest());
    }

    private DataPoint stubDataPoint() {
        ItemMetric grocery = ItemMetric.builder()
                .type(ItemType.EXPENSE)
                .title(GROCERY)
                .moneyAmount(GROCERY_MONEY)
                .build();
        ItemMetric salary = ItemMetric.builder()
                .type(ItemType.INCOME)
                .title(SALARY)
                .moneyAmount(SALARY_MONEY)
                .build();

        return DataPoint.builder()
                .account(ACCOUNT_NAME)
                .date(NOW)
                .metric(grocery)
                .metric(salary)
                .saving(Money.of(SAVING_AMOUNT, BASE_CURRENCY))
                .build();
    }

    private byte[] makeAccountBalsnceJson() throws Exception {
        Item grocery = Item.builder()
                .title(GROCERY)
                .moneyAmount(GROCERY_MONEY)
                .period(TimePeriod.DAY)
                .build();
        Item salary = Item.builder()
                .title(SALARY)
                .moneyAmount(SALARY_MONEY)
                .period(TimePeriod.DAY)
                .build();
        AccountBalance balance = AccountBalance.builder()
                .expense(grocery)
                .income(salary)
                .saving(Saving.of(SAVING_MONEY))
                .build();

        return objectMapper.writeValueAsBytes(balance);
    }

    @TestConfiguration
    @EnableWebSecurity
    static class Config extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest().permitAll()
                    .and()
                    .csrf().disable();
        }
    }
}