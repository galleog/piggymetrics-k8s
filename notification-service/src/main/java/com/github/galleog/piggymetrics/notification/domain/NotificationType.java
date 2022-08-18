package com.github.galleog.piggymetrics.notification.domain;

import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Enumeration for notification types.
 */
@Getter
public enum NotificationType {
    /**
     * Backup notification.
     */
    BACKUP("backup.email.subject", "backup.email.text", "backup.email.attachment"),
    /**
     * Reminder notification.
     */
    REMIND("remind.email.subject", "remind.email.text", null);

    /**
     * Property that defines the notification email subject.
     */
    private String subject;
    /**
     * Property that defines the notification email text.
     */
    private String text;
    /**
     * Property that defines an attachment sent with the notification email.
     */
    private String attachment;

    NotificationType(@NonNull String subject, @NonNull String text, @Nullable String attachment) {
        setSubject(subject);
        setText(text);
        setAttachment(attachment);
    }

    private void setSubject(String subject) {
        Validate.notBlank(subject);
        this.subject = subject;
    }

    private void setText(String text) {
        Validate.notBlank(text);
        this.text = text;
    }

    private void setAttachment(String attachment) {
        this.attachment = attachment;
    }
}
