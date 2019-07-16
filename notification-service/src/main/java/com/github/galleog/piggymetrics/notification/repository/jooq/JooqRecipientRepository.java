package com.github.galleog.piggymetrics.notification.repository.jooq;

import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENTS;
import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENT_NOTIFICATIONS;

import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.domain.tables.records.RecipientNotificationsRecord;
import com.github.galleog.piggymetrics.notification.domain.tables.records.RecipientsRecord;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link RecipientRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqRecipientRepository implements RecipientRepository {
    private final DSLContext dsl;

    @Override
    @Transactional(readOnly = true)
    public Optional<Recipient> getByUsername(@NonNull String username) {
        Validate.notNull(username);

        Result<Record> records = dsl.select()
                .from(RECIPIENTS)
                .leftJoin(RECIPIENT_NOTIFICATIONS).on(RECIPIENT_NOTIFICATIONS.USERNAME.eq(RECIPIENTS.USERNAME))
                .where(RECIPIENTS.USERNAME.eq(username))
                .fetch();
        return records.isEmpty() ? Optional.empty() : Optional.of(toRecipient(records));
    }

    @Override
    @Transactional
    public void save(@NonNull Recipient recipient) {
        Validate.notNull(recipient);

        RecipientsRecord record = dsl.newRecord(RECIPIENTS);
        record.from(recipient);
        record.insert();

        insertNotifications(recipient);
    }

    @Override
    @Transactional
    public Optional<Recipient> update(@NonNull Recipient recipient) {
        Validate.notNull(recipient);

        RecipientsRecord record = dsl.selectFrom(RECIPIENTS)
                .where(RECIPIENTS.USERNAME.eq(recipient.getUsername()))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }

        record.setEmail(recipient.getEmail());
        record.update();

        dsl.deleteFrom(RECIPIENT_NOTIFICATIONS)
                .where(RECIPIENT_NOTIFICATIONS.USERNAME.eq(recipient.getUsername()))
                .execute();

        Map<NotificationType, NotificationSettings> notifications = insertNotifications(recipient).stream()
                .collect(ImmutableMap.toImmutableMap(
                        rec -> rec.get(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE),
                        rec -> rec.into(NotificationSettings.class)
                ));
        return Optional.of(Recipient.builder()
                .username(record.getUsername())
                .email(record.getEmail())
                .notifications(notifications)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recipient> readyToNotify(@NonNull NotificationType type, @NonNull LocalDate date) {
        Validate.notNull(type);
        Validate.notNull(date);

        Map<String, Result<Record>> records = dsl.select()
                .from(RECIPIENTS)
                .leftJoin(RECIPIENT_NOTIFICATIONS).on(RECIPIENT_NOTIFICATIONS.USERNAME.eq(RECIPIENTS.USERNAME))
                .where(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.eq(type))
                .and(RECIPIENT_NOTIFICATIONS.ACTIVE.eq(true))
                .and(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.isNull()
                        .or((RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.plus(RECIPIENT_NOTIFICATIONS.FREQUENCY).lessThan(date))))
                .fetch()
                .intoGroups(RECIPIENTS.USERNAME);
        return records.values().stream()
                .map(this::toRecipient)
                .collect(ImmutableList.toImmutableList());
    }

    private Recipient toRecipient(Result<Record> records) {
        Map<NotificationType, NotificationSettings> notifications = records.stream()
                .filter(r -> r.get(RECIPIENT_NOTIFICATIONS.USERNAME) != null)
                .collect(ImmutableMap.toImmutableMap(
                        r -> r.get(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE),
                        r -> r.into(NotificationSettings.class)
                ));

        Record record = records.get(0);
        return Recipient.builder()
                .username(record.get(RECIPIENTS.USERNAME))
                .email(record.get(RECIPIENTS.EMAIL))
                .notifications(notifications)
                .build();
    }

    private List<RecipientNotificationsRecord> insertNotifications(Recipient recipient) {
        return recipient.getNotifications().entrySet()
                .stream()
                .map(entry -> {
                    RecipientNotificationsRecord record = dsl.newRecord(RECIPIENT_NOTIFICATIONS);
                    record.from(entry.getValue());
                    record.setUsername(recipient.getUsername());
                    record.setNotificationType(entry.getKey());
                    record.setFrequency(entry.getValue().getFrequency().getKey());
                    record.insert();
                    return record;
                }).collect(ImmutableList.toImmutableList());
    }
}
