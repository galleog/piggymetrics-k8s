package com.github.galleog.sk8s.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.galleog.sk8s.notification.domain.Frequency;
import com.github.galleog.sk8s.notification.domain.NotificationSettings;
import com.github.galleog.sk8s.notification.domain.NotificationType;
import com.github.galleog.sk8s.notification.domain.Recipient;
import com.github.galleog.sk8s.notification.service.RecipientService;
import com.google.common.collect.ImmutableMap;
import com.sun.security.auth.UserPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class RecipientControllerTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private RecipientController recipientController;

    @Mock
    private RecipientService recipientService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(recipientController).build();
    }

    @Test
    public void shouldSaveCurrentRecipientSettings() throws Exception {

        Recipient recipient = getStubRecipient();
        String json = mapper.writeValueAsString(recipient);

        mockMvc.perform(put("/recipients/current").principal(new UserPrincipal(recipient.getAccountName())).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetCurrentRecipientSettings() throws Exception {

        Recipient recipient = getStubRecipient();
        when(recipientService.findByAccountName(recipient.getAccountName())).thenReturn(recipient);

        mockMvc.perform(get("/recipients/current").principal(new UserPrincipal(recipient.getAccountName())))
                .andExpect(jsonPath("$.accountName").value(recipient.getAccountName()))
                .andExpect(status().isOk());
    }

    private Recipient getStubRecipient() {

        NotificationSettings remind = new NotificationSettings();
        remind.setActive(true);
        remind.setFrequency(Frequency.WEEKLY);
        remind.setLastNotified(null);

        NotificationSettings backup = new NotificationSettings();
        backup.setActive(false);
        backup.setFrequency(Frequency.MONTHLY);
        backup.setLastNotified(null);

        Recipient recipient = new Recipient();
        recipient.setAccountName("test");
        recipient.setEmail("test@test.com");
        recipient.setScheduledNotifications(ImmutableMap.of(
                NotificationType.BACKUP, backup,
                NotificationType.REMIND, remind
        ));

        return recipient;
    }
}