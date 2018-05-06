package com.github.galleog.sk8s.account.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.sk8s.account.AccountApplication;
import com.github.galleog.sk8s.account.domain.*;
import com.github.galleog.sk8s.account.service.AccountService;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

import static com.github.galleog.sk8s.account.domain.TimePeriod.DAY;
import static com.github.galleog.sk8s.account.domain.TimePeriod.MONTH;
import static com.github.galleog.sk8s.account.service.AccountService.DEFAULT_CURRENCY;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AccountController.class)
@ContextConfiguration(classes = {
        AccountApplication.class,
        AccountControllerTest.WebSecurityConfig.class
})
@WithMockUser(AccountControllerTest.ACCOUNT_NAME)
public class AccountControllerTest {
    static final String ACCOUNT_NAME = "test";
    private static final String ACCOUNT_NOTE = "note";
    private static final double SAVING_AMOUNT = 1500;
    private static final double SAVING_INTEREST = 3.32;
    private static final String GROCERY = "Grocery";
    private static final double GROCERY_AMOUNT = 10;
    private static final String GROCERY_ICON = "meal";
    private static final String SALARY = "Salary";
    private static final double SALARY_AMOUNT = 9100;
    private static final String SALARY_ICON = "wallet";

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
    public void shouldGetAccountByName() throws Exception {
        Account account = stubAccount();
        when(accountService.findByName(ACCOUNT_NAME)).thenReturn(Optional.of(account));
        mockMvc.perform(get("/" + ACCOUNT_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME))
                .andExpect(jsonPath("$.note").value(ACCOUNT_NOTE))
                .andExpect(jsonPath("$.balance.saving.id").doesNotExist())
                .andExpect(jsonPath("$.balance.saving.moneyAmount.amount").value(SAVING_AMOUNT))
                .andExpect(jsonPath("$.balance.saving.moneyAmount.currency")
                        .value(DEFAULT_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.balance.saving.interest").value(SAVING_INTEREST))
                .andExpect(jsonPath("$.balance.saving.deposit").value(true))
                .andExpect(jsonPath("$.balance.saving.capitalization").value(false))
                .andExpect(jsonPath("$.balance.expenses").value(hasSize(1)))
                .andExpect(jsonPath("$.balance.expenses[0].id").doesNotExist())
                .andExpect(jsonPath("$.balance.expenses[0].title").value(GROCERY))
                .andExpect(jsonPath("$.balance.expenses[0].moneyAmount.amount").value(GROCERY_AMOUNT))
                .andExpect(jsonPath("$.balance.expenses[0].moneyAmount.currency")
                        .value(DEFAULT_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.balance.expenses[0].period").value(DAY.name()))
                .andExpect(jsonPath("$.balance.expenses[0].icon").value(GROCERY_ICON))
                .andExpect(jsonPath("$.balance.incomes").value(hasSize(1)))
                .andExpect(jsonPath("$.balance.incomes[0].id").doesNotExist())
                .andExpect(jsonPath("$.balance.incomes[0].title").value(SALARY))
                .andExpect(jsonPath("$.balance.incomes[0].moneyAmount.amount").value(SALARY_AMOUNT))
                .andExpect(jsonPath("$.balance.incomes[0].moneyAmount.currency")
                        .value(DEFAULT_CURRENCY.getCurrencyCode()))
                .andExpect(jsonPath("$.balance.incomes[0].period").value(MONTH.name()))
                .andExpect(jsonPath("$.balance.incomes[0].icon").value(SALARY_ICON));
    }

    /**
     * Test for {@link AccountController#getAccountByName(String)} if there exists no account with the given name.
     */
    @Test
    public void shouldFailIfAccountDoesNotExist() throws Exception {
        when(accountService.findByName(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get("/" + ACCOUNT_NAME)).andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#getCurrentAccount(Principal)}.
     */
    @Test
    public void shouldGetCurrentAccount() throws Exception {
        Account account = stubAccount();
        when(accountService.findByName(ACCOUNT_NAME)).thenReturn(Optional.of(account));
        mockMvc.perform(get("/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME));
    }

    /**
     * Test for {@link AccountController#getCurrentAccount(Principal)}
     * if there exists no account with the principal's name.
     */
    @Test
    public void shouldFailIfNoAccountWithPrincipalNameExistsInGetCurrentAccount() throws Exception {
        when(accountService.findByName(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get("/current")).andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#updateCurrentAccount(Principal, Account)}.
     */
    @Test
    public void shouldUpdateCurrentAccount() throws Exception {
        Account account = stubAccount();
        when(accountService.update(eq(ACCOUNT_NAME), any(Account.class))).thenReturn(Optional.of(account));

        String json = objectMapper.writeValueAsString(account);
        mockMvc.perform(put("/current").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNoContent());
    }

    /**
     * Test for {@link AccountController#updateCurrentAccount(Principal, Account)}
     * if there exists no account with the principal's name.
     */
    @Test
    public void shouldFailIfNoAccountWithPrincipalNameExistsInUpdateCurrentAccount() throws Exception {
        when(accountService.update(anyString(), any(Account.class))).thenReturn(Optional.empty());

        String json = objectMapper.writeValueAsString(stubAccount());
        mockMvc.perform(put("/current").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    /**
     * Test for {@link AccountController#createNewAccount(User)}.
     */
    @Test
    public void shouldCreateNewAccount() throws Exception {
        when(accountService.create(any(User.class))).thenReturn(stubAccount());
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(ACCOUNT_NAME));
    }

    /**
     * Test for {@link AccountController#processValidationError(Exception)}.
     */
    @Test
    public void shouldReturnHTTP400BadRequest() throws Exception {
        doThrow(IllegalArgumentException.class).when(accountService).create(any(User.class));
        mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isBadRequest());
    }

    private Account stubAccount() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(SAVING_AMOUNT, DEFAULT_CURRENCY))
                .interest(BigDecimal.valueOf(SAVING_INTEREST))
                .deposit(true)
                .build();

        Expense grocery = Expense.builder()
                .title(GROCERY)
                .moneyAmount(Money.of(GROCERY_AMOUNT, DEFAULT_CURRENCY))
                .period(DAY)
                .icon(GROCERY_ICON)
                .build();
        Income salary = Income.builder()
                .title(SALARY)
                .moneyAmount(Money.of(SALARY_AMOUNT, DEFAULT_CURRENCY))
                .period(MONTH)
                .icon(SALARY_ICON)
                .build();

        AccountBalance balance = AccountBalance.builder()
                .expense(grocery)
                .income(salary)
                .saving(saving)
                .build();

        return Account.builder()
                .name(ACCOUNT_NAME)
                .balance(balance)
                .note(ACCOUNT_NOTE)
                .build();
    }

    private String makeUserJson() throws JsonProcessingException {
        User user = User.builder()
                .username(ACCOUNT_NAME)
                .password("secret")
                .build();
        return objectMapper.writeValueAsString(user);
    }

    @Configuration
    @EnableWebSecurity
    static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest().permitAll()
                    .and()
                    .csrf().disable();
        }
    }
}
