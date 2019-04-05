package com.github.galleog.piggymetrics.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.time.LocalDate;

/**
 * Settings for notifications of a particular {@link NotificationType}.
 */
@Getter
@Embeddable
@JsonDeserialize(builder = NotificationSettings.NotificationSettingsBuilder.class)
public class NotificationSettings {
    /**
     * Indicates if the notification is active.
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
    private LocalDate lastNotifiedDate;

    @SuppressWarnings("unused")
    NotificationSettings() {
    }

    @Builder
    @SuppressWarnings("unused")
    private NotificationSettings(@NonNull boolean active, @NonNull Frequency frequency, @Nullable LocalDate lastNotifiedDate) {
        setActive(active);
        setFrequency(frequency);
        setLastNotifiedDate(lastNotifiedDate);
    }

    /**
     * Indicates if the recipient is notified.
     *
     * @return {@code true} if the recipient is notified; {@code false} otherwise
     */
    @Transient
    @JsonIgnore
    public boolean isNotified() {
        return getLastNotifiedDate() != null;
    }

    /**
     * Sets the last notified date to the current date.
     */
    void setNotified() {
        setLastNotifiedDate(LocalDate.now());
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    private void setFrequency(Frequency frequency) {
        Validate.notNull(frequency);
        this.frequency = frequency;
    }

    private void setLastNotifiedDate(LocalDate date) {
        this.lastNotifiedDate = date;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class NotificationSettingsBuilder {
    }
}
