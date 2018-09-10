package com.github.galleog.piggymetrics.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.galleog.piggymetrics.core.domain.AbstractSequencePersistable;
import com.github.galleog.piggymetrics.core.enums.hibernate.AbstractHibernateEnumType;
import com.github.galleog.piggymetrics.core.enums.hibernate.HibernateIntegerEnumType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.validator.routines.EmailValidator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.AttributeOverride;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Recipient of notifications.
 */
@Entity
@Table(name = Recipient.TABLE_NAME)
@JsonIgnoreProperties({"id", "new", "version"})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@TypeDef(
        defaultForType = Frequency.class,
        typeClass = HibernateIntegerEnumType.class,
        parameters = @Parameter(name = AbstractHibernateEnumType.PARAMETER_NAME, value = Frequency.CLASS_NAME)
)
public class Recipient extends AbstractSequencePersistable<Integer> {
    @VisibleForTesting
    public static final String TABLE_NAME = "recipients";
    @VisibleForTesting
    public static final String NOTIFICATIONS_TABLE_NAME = "recipient_notifications";

    /**
     * Recipient's account name.
     */
    @NonNull
    @Getter
    @NaturalId
    @Column(name = "account_name")
    private String accountName;
    /**
     * Email to send notifications to.
     */
    @Getter
    @NonNull
    private String email;
    /**
     * Notification settings.
     */
    @NonNull
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "notification_type")
    @CollectionTable(name = NOTIFICATIONS_TABLE_NAME, joinColumns = @JoinColumn(name = "recipient_id"))
    @AttributeOverride(name = "lastNotifiedDate", column = @Column(name = "last_notified_date"))
    private Map<NotificationType, NotificationSettings> scheduledNotifications = new HashMap<>();

    @Version
    @SuppressWarnings("unused")
    private Integer version;

    @Builder
    @SuppressWarnings("unused")
    private Recipient(@NonNull String accountName, @NonNull String email,
                      @Nullable @Singular Map<NotificationType, NotificationSettings> scheduledNotifications) {
        setAccountName(accountName);
        setEmail(email);
        setScheduledNotifications(scheduledNotifications);
    }

    /**
     * Updates the recipient notification settings.
     *
     * @param email    the new recipient email
     * @param settings the new notification settings
     * @throws NullPointerException     if the settings are {@code null}
     * @throws IllegalArgumentException if the email is invalid or the settings contain {@code null} values
     */
    public void updateSettings(@NonNull String email, @NonNull Map<NotificationType, NotificationSettings> settings) {
        setEmail(email);
        setScheduledNotifications(settings);
    }

    /**
     * Updates the last notified date of this recipient.
     *
     * @param type the notification type
     * @throws NullPointerException     if the type is {@code null}
     * @throws IllegalArgumentException if the notification settings of the specified type aren't active
     */
    public void markNotified(@NonNull NotificationType type) {
        Validate.notNull(type);
        Optional.ofNullable(getScheduledNotifications().get(type))
                .ifPresent(settings -> {
                    Validate.isTrue(settings.isActive());
                    settings.setNotified();
                });
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(accountName)
                .build();
    }

    private void setAccountName(String accountName) {
        Validate.notBlank(accountName);
        this.accountName = accountName;
    }

    private void setEmail(String email) {
        Validate.isTrue(EmailValidator.getInstance().isValid(email));
        this.email = email;
    }

    /**
     * Gets scheduled notifications of this recipient.
     */
    @NonNull
    public Map<NotificationType, NotificationSettings> getScheduledNotifications() {
        return ImmutableMap.copyOf(this.scheduledNotifications);
    }

    private void setScheduledNotifications(Map<NotificationType, NotificationSettings> scheduledNotifications) {
        Validate.notNull(scheduledNotifications);
        Validate.noNullElements(scheduledNotifications.values());
        this.scheduledNotifications.clear();
        this.scheduledNotifications.putAll(scheduledNotifications);
    }
}
