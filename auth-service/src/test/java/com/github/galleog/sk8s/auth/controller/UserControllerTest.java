package com.github.galleog.sk8s.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.sk8s.auth.AuthApplication;
import com.github.galleog.sk8s.auth.domain.User;
import com.github.galleog.sk8s.auth.service.UserService;
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

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link UserController}.
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = {
        AuthApplication.class,
        UserControllerTest.WebSecurityConfig.class
})
@WithMockUser(UserControllerTest.USERNAME)
public class UserControllerTest {
    private static final String BASE_URL = "/users";
    static final String USERNAME = "test";
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
    public void shouldCreateNewUser() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isCreated());
    }

    /**
     * Test for {@link UserController#createUser(User)} when user details aren't specified.
     */
    @Test
    public void shouldFailToCreateUserWithoutUserDetails() throws Exception {
        mockMvc.perform(post(BASE_URL))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test for {@link UserController#currentPrincipal(Principal)}.
     */
    @Test
    public void shouldReturnCurrentPrincipal() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(USERNAME));
    }

    /**
     * Test for {@link UserController#processValidationError(Exception)}.
     */
    @Test
    public void shouldReturnHTTP400BadRequest() throws Exception {
        doThrow(IllegalArgumentException.class).when(userService).create(any(User.class));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(makeUserJson()))
                .andExpect(status().isBadRequest());
    }

    private String makeUserJson() throws JsonProcessingException {
        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
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
