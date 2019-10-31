package com.github.galleog.piggymetrics.apigateway.model.notification;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * Settings for notifications of a particular {@link NotificationType}.
 */
@Getter
@JsonDeserialize(builder = NotificationSettings.NotificationSettingsBuilder.class)
public class NotificationSettings {
    /**
     * Indicates if the notification is active. Default is {@code true}.
     */
    @Builder.Default
    private boolean active = true;
    /**
     * Notification frequency.
     */
    @NonNull
    private Frequency frequency;
    /**
     * Date when the notification was last sent.
     */
    @Nullable
    private LocalDate notifyDate;

    @Builder
    @SuppressWarnings("unused")
    private NotificationSettings(boolean active, @NonNull Frequency frequency, @Nullable LocalDate notifyDate) {
        setActive(active);
        setFrequency(frequency);
        setNotifyDate(notifyDate);
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    private void setFrequency(Frequency frequency) {
        Validate.notNull(frequency);
        this.frequency = frequency;
    }

    private void setNotifyDate(LocalDate date) {
        this.notifyDate = date;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class NotificationSettingsBuilder {
    }
}
