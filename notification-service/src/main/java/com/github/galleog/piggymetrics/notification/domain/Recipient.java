package com.github.galleog.piggymetrics.notification.domain;

import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Recipient of notifications.
 */
@Getter
public class Recipient {
    /**
     * Name of the user to send notifications to.
     */
    private String username;
    /**
     * Email to send notifications to.
     */
    private String email;
    /**
     * Notification settings.
     */
    private Map<NotificationType, NotificationSettings> notifications;

    @Builder
    @SuppressWarnings("unused")
    private Recipient(@NonNull String username, @NonNull String email,
                      @NonNull @Singular Map<NotificationType, NotificationSettings> notifications) {
        setUsername(username);
        setEmail(email);
        setNotifications(notifications);
    }

    /**
     * Returns a new recipient whose last notified date for the specified notification type set to the current date.
     *
     * @param type the notification type
     * @throws NullPointerException     if the type is {@code null}
     * @throws IllegalArgumentException if the notification settings of the specified type aren't active
     */
    public Recipient markNotified(@NonNull NotificationType type) {
        Validate.notNull(type);

        var builder = Recipient.builder()
                .username(getUsername())
                .email(getEmail())
                .notifications(getNotifications());

        var settings = getNotifications().get(type);
        if (settings != null) {
            builder.notification(type, settings.markNotified());
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(getUsername())
                .build();
    }

    private void setUsername(String username) {
        Validate.notBlank(username);
        this.username = username;
    }

    private void setEmail(String email) {
        Validate.isTrue(EmailValidator.getInstance().isValid(email));
        this.email = email;
    }

    private void setNotifications(Map<NotificationType, NotificationSettings> notifications) {
        Validate.notNull(notifications);
        Validate.noNullElements(notifications.values());
        this.notifications = ImmutableMap.copyOf(notifications);
    }
}
