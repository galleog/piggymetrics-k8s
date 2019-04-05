package com.github.galleog.piggymetrics.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.piggymetrics.notification.acl.RecipientSettings;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.service.RecipientService;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for {@link RecipientController}.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@WebMvcTest(RecipientController.class)
@WithMockUser(RecipientControllerTest.ACCOUNT_NAME)
class RecipientControllerTest {
    static final String ACCOUNT_NAME = "test";
    private static final String URL = "/recipients/current";
    private static final String EMAIL = "test@example.com";
    private static final LocalDate BACKUP_DATE = LocalDate.now().minusDays(10);
    private static final LocalDate REMIND_DATE = LocalDate.now().minusDays(3);

    @MockBean
    private RecipientService recipientService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    /**
     * Test for {@link RecipientController#getCurrentNotificationsSettings(Principal)}.
     */
    @Test
    void shouldGetCurrentRecipientSettings() throws Exception {
        when(recipientService.findByAccountName(ACCOUNT_NAME)).thenReturn(Optional.of(stubRecipient()));
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.accountName").value(ACCOUNT_NAME))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.scheduledNotifications.BACKUP.active").value(false))
                .andExpect(jsonPath("$.scheduledNotifications.BACKUP.frequency").value(Frequency.MONTHLY.getKey()))
                .andExpect(jsonPath("$.scheduledNotifications.BACKUP.lastNotifiedDate")
                        .value(BACKUP_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .andExpect(jsonPath("$.scheduledNotifications.REMIND.active").value(true))
                .andExpect(jsonPath("$.scheduledNotifications.REMIND.frequency").value(Frequency.WEEKLY.getKey()))
                .andExpect(jsonPath("$.scheduledNotifications.REMIND.lastNotifiedDate")
                        .value(REMIND_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }

    /**
     * Test for {@link RecipientController#getCurrentNotificationsSettings(Principal)}
     * when there is no recipient with the name of the current principal.
     */
    @Test
    void shouldFailToGetNotificationSettingsIfRecipientDoesNotExist() throws Exception {
        when(recipientService.findByAccountName(ACCOUNT_NAME)).thenReturn(Optional.empty());
        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound());
    }

    /**
     * Test for {@link RecipientController#saveCurrentNotificationsSettings(Principal, RecipientSettings)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveCurrentRecipientSettings() throws Exception {
        ArgumentCaptor<Map<NotificationType, NotificationSettings>> captor = ArgumentCaptor.forClass(Map.class);
        when(recipientService.save(eq(ACCOUNT_NAME), eq(EMAIL), captor.capture())).thenReturn(stubRecipient());

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(makeRecipientSettingsJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value(ACCOUNT_NAME));

        assertThat(captor.getValue().get(NotificationType.BACKUP)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(false, Frequency.MONTHLY, BACKUP_DATE);
        assertThat(captor.getValue().get(NotificationType.REMIND)).extracting(
                NotificationSettings::isActive,
                NotificationSettings::getFrequency,
                NotificationSettings::getLastNotifiedDate
        ).containsExactly(true, Frequency.WEEKLY, REMIND_DATE);
    }

    /**
     * Test for {@link RecipientController#processValidationError(Exception)}.
     */
    @Test
    void shouldReturnHTTP400BadRequest() throws Exception {
        when(recipientService.save(eq(ACCOUNT_NAME), eq(EMAIL), anyMap())).thenThrow(IllegalArgumentException.class);
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(makeRecipientSettingsJson()))
                .andExpect(status().isBadRequest());
    }

    private Recipient stubRecipient() {
        return Recipient.builder()
                .accountName(ACCOUNT_NAME)
                .email(EMAIL)
                .scheduledNotifications(stubNotificationSettings())
                .build();
    }

    private Map<NotificationType, NotificationSettings> stubNotificationSettings() {
        NotificationSettings backup = NotificationSettings.builder()
                .active(false)
                .frequency(Frequency.MONTHLY)
                .lastNotifiedDate(BACKUP_DATE)
                .build();
        NotificationSettings remind = NotificationSettings.builder()
                .active(true)
                .frequency(Frequency.WEEKLY)
                .lastNotifiedDate(REMIND_DATE)
                .build();
        return ImmutableMap.of(
                NotificationType.BACKUP, backup,
                NotificationType.REMIND, remind
        );
    }

    private byte[] makeRecipientSettingsJson() throws JsonProcessingException {
        RecipientSettings settings = RecipientSettings.builder()
                .email(EMAIL)
                .scheduledNotifications(stubNotificationSettings())
                .build();
        return objectMapper.writeValueAsBytes(settings);
    }

    @TestConfiguration
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