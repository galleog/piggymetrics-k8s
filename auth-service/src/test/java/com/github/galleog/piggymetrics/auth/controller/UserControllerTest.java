package com.github.galleog.piggymetrics.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.piggymetrics.auth.domain.User;
import com.github.galleog.piggymetrics.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/**
 * Tests for {@link UserController}.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@WithMockUser(UserControllerTest.USERNAME)
class UserControllerTest {
    static final String USERNAME = "test";
    private static final String BASE_URL = "/users";
    private static final String PASSWORD = "secret";

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    /**
     * Test for {@link UserController#createUser(User)}.
     */
    @Test
    void shouldCreateNewUser() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isCreated());
        verify(userService).create(any(User.class));
    }

    /**
     * Test for {@link UserController#createUser(User)} when user details aren't specified.
     */
    @Test
    void shouldFailToCreateUserWithoutUserDetails() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test for {@link UserController#currentPrincipal(Principal)}.
     */
    @Test
    void shouldReturnCurrentPrincipal() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(USERNAME));
    }

    /**
     * Test for {@link UserController#processValidationError(Exception)}.
     */
    @Test
    void shouldReturnHTTP400BadRequest() throws Exception {
        doThrow(IllegalArgumentException.class).when(userService).create(any(User.class));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isBadRequest());
    }

    private byte[] makeUserJson() throws JsonProcessingException {
        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
        return objectMapper.writeValueAsBytes(user);
    }

    @TestConfiguration
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
