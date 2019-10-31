package com.github.galleog.piggymetrics.apigateway.model.notification;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Recipient of notifications.
 */
@Getter
@JsonDeserialize(builder = Recipient.RecipientBuilder.class)
public class Recipient {
    /**
     * Name of the user to send notifications to.
     */
    @Nullable
    private String username;
    /**
     * Email to send notifications to.
     */
    @NonNull
    private String email;
    /**
     * Notification settings.
     */
    @NonNull
    private Map<NotificationType, NotificationSettings> notifications;

    @Builder
    @SuppressWarnings("unused")
    private Recipient(@Nullable String username, @NonNull String email,
                      @NonNull @Singular Map<NotificationType, NotificationSettings> notifications) {
        setUsername(username);
        setEmail(email);
        setNotifications(notifications);
    }

    private void setUsername(String username) {
        this.username = username;
    }

    private void setEmail(String email) {
        Validate.notNull(email);
        this.email = email;
    }

    private void setNotifications(Map<NotificationType, NotificationSettings> notifications) {
        Validate.notNull(notifications);
        Validate.noNullElements(notifications.values());
        this.notifications = ImmutableMap.copyOf(notifications);
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class RecipientBuilder {
    }
}
