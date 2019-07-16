package com.github.galleog.piggymetrics.notification.repository.jooq;

import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENT_NOTIFICATIONS;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import org.jooq.Record;
import org.jooq.RecordMapper;

/**
 * {@link RecordMapper} for {@link NotificationSettings}.
 */
public class NotificationSettingsRecordMapper<R extends Record> implements RecordMapper<R, NotificationSettings> {
    @Override
    public NotificationSettings map(R record) {
        return NotificationSettings.builder()
                .active(record.get(RECIPIENT_NOTIFICATIONS.ACTIVE))
                .frequency(Frequency.valueOf(record.get(RECIPIENT_NOTIFICATIONS.FREQUENCY)))
                .notifyDate(record.get(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE))
                .build();
    }
}
