package com.github.galleog.piggymetrics.notification.domain;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Settings for notifications of a particular {@link NotificationType}.
 */
@Getter
public class NotificationSettings {
    /**
     * Indicates if the notification is active. Default is {@code true}.
     */
    @Builder.Default
    private boolean active = true;
    /**
     * Notification frequency.
     */
    private Frequency frequency;
    /**
     * Date when the notification was last sent.
     */
    private LocalDate notifyDate;

    @Builder
    @SuppressWarnings("unused")
    private NotificationSettings(boolean active, @NonNull Frequency frequency, @Nullable LocalDate notifyDate) {
        setActive(active);
        setFrequency(frequency);
        setNotifyDate(notifyDate);
    }

    /**
     * Indicates if the recipient is notified.
     *
     * @return {@code true} if the recipient is notified; {@code false} otherwise
     */
    public boolean isNotified() {
        return this.getNotifyDate() != null;
    }

    /**
     * Returns new notification settings with the notified date set to the current date.
     *
     * @throws IllegalArgumentException if the notification settings aren't active
     */
    public NotificationSettings markNotified() {
        Validate.isTrue(isActive());

        return NotificationSettings.builder()
                .active(true)
                .frequency(getFrequency())
                .notifyDate(LocalDate.now())
                .build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("active", isActive())
                .append("frequency", getFrequency())
                .append("notifyDate", getNotifyDate() == null ? null : DateTimeFormatter.ISO_DATE.format(getNotifyDate()))
                .build();
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
}
