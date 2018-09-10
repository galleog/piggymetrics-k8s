package com.github.galleog.piggymetrics.account.controller;

import static com.github.galleog.piggymetrics.account.domain.TimePeriod.DAY;
import static com.github.galleog.piggymetrics.account.domain.TimePeriod.MONTH;
import static com.github.galleog.piggymetrics.account.service.AccountService.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.piggymetrics.account.acl.AccountConverter;
import com.github.galleog.piggymetrics.account.acl.AccountDto;
import com.github.galleog.piggymetrics.account.acl.User;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Expense;
import com.github.galleog.piggymetrics.account.domain.Income;
import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.service.AccountService;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link AccountController}.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@WebMvcTest(AccountController.class)
@WithMockUser(AccountControllerTest.ACCOUNT_NAME)
class AccountControllerTest {
    static final String ACCOUNT_NAME = "test";
    private static final String BASE_URL = "/accounts";
    private static final String ACCOUNT_NOTE = "note";
    private static final double SAVING_AMOUNT = 1500;
    private static final double SAVING_INTEREST = 3.32;
    private static final String GROCERY = "Grocery";
    private static final double GROCERY_AMOUNT = 10;
    private static final String GROCERY_ICON = "meal";
    private static final String SALARY = "Salary";
    private static final double SALARY_AMOUNT = 9100;
    private static final String SALARY_ICON = "wallet";
    private static final String PASSWORD = "secret";

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    /**
     * Test for {@link AccountController#getAccountByName(String)}.
     */
    @Test
    void shouldGetAccountByName() throws Exception {
        when(accountService.findByName(ACCOUNT_NAME)).thenReturn(Optional.of(stubAccount()));
        mockMvc.perform(get(BASE_URL + "/" + ACCOUNT_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME))
                .andExpect(jsonPath("$.note").value(ACCOUNT_NOTE))
                .andExpect(jsonPath("$.lastModifiedDate").isString())
                .andExpect(jsonPath("$.saving.id").doesNotExist())
                .andExpect(jsonPath("$.saving.moneyAmount.amount").value(SAVING_AMOUNT))
                .andExpect(jsonPath("$.saving.moneyAmount.currency").value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.saving.interest").value(SAVING_INTEREST))
                .andExpect(jsonPath("$.saving.deposit").value(true))
                .andExpect(jsonPath("$.saving.capitalization").value(false))
                .andExpect(jsonPath("$.expenses").value(hasSize(1)))
                .andExpect(jsonPath("$.expenses[0].id").doesNotExist())
                .andExpect(jsonPath("$.expenses[0].title").value(GROCERY))
                .andExpect(jsonPath("$.expenses[0].moneyAmount.amount").value(GROCERY_AMOUNT))
                .andExpect(jsonPath("$.expenses[0].moneyAmount.currency").value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.expenses[0].period").value(DAY.name()))
                .andExpect(jsonPath("$.expenses[0].icon").value(GROCERY_ICON))
                .andExpect(jsonPath("$.incomes").value(hasSize(1)))
                .andExpect(jsonPath("$.incomes[0].id").doesNotExist())
                .andExpect(jsonPath("$.incomes[0].title").value(SALARY))
                .andExpect(jsonPath("$.incomes[0].moneyAmount.amount").value(SALARY_AMOUNT))
                .andExpect(jsonPath("$.incomes[0].moneyAmount.currency").value(BASE_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.incomes[0].period").value(MONTH.name()))
                .andExpect(jsonPath("$.incomes[0].icon").value(SALARY_ICON));
    }

    /**
     * Test for {@link AccountController#getAccountByName(String)} if there exists no account with the given name.
     */
    @Test
    void shouldFailIfAccountDoesNotExist() throws Exception {
        when(accountService.findByName(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get(BASE_URL + "/" + ACCOUNT_NAME)).andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#getCurrentAccount(Principal)}.
     */
    @Test
    void shouldGetCurrentAccount() throws Exception {
        when(accountService.findByName(ACCOUNT_NAME)).thenReturn(Optional.of(stubAccount()));
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME));
    }

    /**
     * Test for {@link AccountController#getCurrentAccount(Principal)}
     * if there exists no account with the principal's name.
     */
    @Test
    void shouldFailIfNoAccountWithPrincipalNameExistsInGetCurrentAccount() throws Exception {
        when(accountService.findByName(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get(BASE_URL + "/current")).andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#updateCurrentAccount(Principal, AccountDto)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateCurrentAccount() throws Exception {
        AccountDto account = stubAccountDto();
        ArgumentCaptor<List<Item>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Saving> savingCaptor = ArgumentCaptor.forClass(Saving.class);
        when(accountService.update(eq(ACCOUNT_NAME), itemsCaptor.capture(), savingCaptor.capture(), eq(ACCOUNT_NOTE)))
                .thenReturn(Optional.of(stubAccount()));

        byte[] json = objectMapper.writeValueAsBytes(account);
        mockMvc.perform(put(BASE_URL + "/current").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent());

        assertThat(itemsCaptor.getValue()).extracting(
                Item::getTitle, Item::getMoneyAmount, Item::getPeriod, Item::getIcon
        ).containsExactlyInAnyOrder(
                tuple(GROCERY, Money.of(GROCERY_AMOUNT, BASE_CURRENCY), DAY, GROCERY_ICON),
                tuple(SALARY, Money.of(SALARY_AMOUNT, BASE_CURRENCY), MONTH, SALARY_ICON)
        );

        Saving saving = savingCaptor.getValue();
        assertThat(saving.getMoneyAmount()).isEqualTo(Money.of(SAVING_AMOUNT, BASE_CURRENCY));
        assertThat(saving.getInterest()).isEqualTo(BigDecimal.valueOf(SAVING_INTEREST));
        assertThat(saving.isDeposit()).isTrue();
        assertThat(saving.isCapitalization()).isFalse();
    }

    /**
     * Test for {@link AccountController#updateCurrentAccount(Principal, AccountDto)}
     * if there exists no account with the principal's name.
     */
    @Test
    void shouldFailIfNoAccountWithPrincipalNameExistsInUpdateCurrentAccount() throws Exception {
        when(accountService.update(anyString(), anyList(), any(Saving.class), anyString())).thenReturn(Optional.empty());

        byte[] json = objectMapper.writeValueAsBytes(stubAccountDto());
        mockMvc.perform(put(BASE_URL + "/current").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#createNewAccount(User)}.
     */
    @Test
    void shouldCreateNewAccount() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(accountService.create(userCaptor.capture())).thenReturn(stubAccount());

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME));

        assertThat(userCaptor.getValue().getUsername()).isEqualTo(ACCOUNT_NAME);
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(PASSWORD);
    }

    /**
     * Test for {@link AccountController#processValidationError(Exception)}.
     */
    @Test
    void shouldReturnHTTP400BadRequest() throws Exception {
        when(accountService.create(any(User.class))).thenThrow(IllegalArgumentException.class);
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isBadRequest());
    }

    private Account stubAccount() {
        return Account.builder()
                .name(ACCOUNT_NAME)
                .item(stubExpense())
                .item(stubIncome())
                .saving(stubSaving())
                .note(ACCOUNT_NOTE)
                .build();
    }

    private AccountDto stubAccountDto() {
        return AccountDto.builder()
                .expense(stubExpense())
                .income(stubIncome())
                .saving(stubSaving())
                .note(ACCOUNT_NOTE)
                .build();
    }

    private Saving stubSaving() {
        return Saving.builder()
                .moneyAmount(Money.of(SAVING_AMOUNT, BASE_CURRENCY))
                .interest(BigDecimal.valueOf(SAVING_INTEREST))
                .deposit(true)
                .build();
    }

    private Expense stubExpense() {
        return Expense.builder()
                .title(GROCERY)
                .moneyAmount(Money.of(GROCERY_AMOUNT, BASE_CURRENCY))
                .period(DAY)
                .icon(GROCERY_ICON)
                .build();
    }

    private Income stubIncome() {
        return Income.builder()
                .title(SALARY)
                .moneyAmount(Money.of(SALARY_AMOUNT, BASE_CURRENCY))
                .period(MONTH)
                .icon(SALARY_ICON)
                .build();
    }

    private byte[] makeUserJson() throws JsonProcessingException {
        User user = User.builder()
                .username(ACCOUNT_NAME)
                .password(PASSWORD)
                .build();
        return objectMapper.writeValueAsBytes(user);
    }

    @TestConfiguration
    @EnableWebSecurity
    static class Config extends WebSecurityConfigurerAdapter {
        @Bean
        public AccountConverter accountConverter() {
            return new AccountConverter();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest().permitAll()
                    .and()
                    .csrf().disable();
        }
    }
}
