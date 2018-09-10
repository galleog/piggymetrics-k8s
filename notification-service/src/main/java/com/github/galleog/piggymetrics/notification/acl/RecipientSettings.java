package com.github.galleog.piggymetrics.notification.acl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Settings of a recipient.
 */
@JsonDeserialize(builder = RecipientSettings.RecipientSettingsBuilder.class)
public class RecipientSettings {
    /**
     * Email to send notifications to.
     */
    @Getter
    @NonNull
    private String email;
    /**
     * Notification settings.
     */
    @Getter
    @NonNull
    private Map<NotificationType, NotificationSettings> scheduledNotifications;

    @Builder
    @SuppressWarnings("unused")
    private RecipientSettings(@NonNull String email,
                              @NonNull @Singular Map<NotificationType, NotificationSettings> scheduledNotifications) {
        setEmail(email);
        setScheduledNotifications(scheduledNotifications);
    }

    private void setEmail(@NonNull String email) {
        Validate.isTrue(EmailValidator.getInstance().isValid(email));
        this.email = email;
    }

    private void setScheduledNotifications(Map<NotificationType, NotificationSettings> scheduledNotifications) {
        Validate.notNull(scheduledNotifications);
        this.scheduledNotifications = ImmutableMap.copyOf(scheduledNotifications);
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class RecipientSettingsBuilder {
    }
}
