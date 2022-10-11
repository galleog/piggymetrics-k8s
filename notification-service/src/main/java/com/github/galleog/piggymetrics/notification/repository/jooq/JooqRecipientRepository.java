package com.github.galleog.piggymetrics.notification.repository.jooq;

import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENTS;
import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENT_NOTIFICATIONS;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.jooq.impl.DSL.select;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.domain.tables.records.RecipientsRecord;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.github.galleog.piggymetrics.autoconfigure.jooq.TransactionAwareJooqWrapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link RecipientRepository} using <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Repository
@RequiredArgsConstructor
public class JooqRecipientRepository implements RecipientRepository {
    private final TransactionAwareJooqWrapper wrapper;

    @Override
    @Transactional(readOnly = true)
    public Mono<Recipient> getByUsername(@NonNull String username) {
        Validate.notNull(username);
        return wrapper.withDSLContextMany(ctx ->
                        ctx.select()
                                .from(RECIPIENTS)
                                .leftJoin(RECIPIENT_NOTIFICATIONS).on(RECIPIENT_NOTIFICATIONS.USERNAME.eq(RECIPIENTS.USERNAME))
                                .where(RECIPIENTS.USERNAME.eq(username))
                ).collectList()
                .mapNotNull(this::toRecipient);
    }

    @Override
    @Transactional
    public Mono<Recipient> save(@NonNull Recipient recipient) {
        Validate.notNull(recipient);
        return insertRecipientSql(recipient)
                .map(record ->
                        Recipient.builder()
                                .username(record.getUsername())
                                .email(record.getEmail())
                ).flatMap(builder ->
                        insertNotifications(recipient)
                                .map(notifications -> builder.notifications(notifications).build())
                );
    }

    @Override
    @Transactional
    public Mono<Recipient> update(@NonNull Recipient recipient) {
        Validate.notNull(recipient);
        return updateRecipientSql(recipient)
                .map(record ->
                        Recipient.builder()
                                .username(record.getUsername())
                                .email(record.getEmail())
                ).flatMap(builder ->
                        deleteNotificationsSql(recipient.getUsername())
                                .map(i -> builder)
                ).flatMap(builder ->
                        insertNotifications(recipient)
                                .map(notifications -> builder.notifications(notifications).build())
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Recipient> readyToNotify(@NonNull NotificationType type, @NonNull LocalDate date) {
        Validate.notNull(type);
        Validate.notNull(date);

        return readyToNotifySql(type, date)
                .bufferUntilChanged(record -> record.get(RECIPIENTS.USERNAME))
                .map(this::toRecipient);
    }

    private Flux<Record> readyToNotifySql(NotificationType type, LocalDate date) {
        return wrapper.withDSLContextMany(ctx ->
                ctx.select()
                        .from(RECIPIENTS)
                        .leftJoin(RECIPIENT_NOTIFICATIONS).on(RECIPIENT_NOTIFICATIONS.USERNAME.eq(RECIPIENTS.USERNAME))
                        .where(RECIPIENTS.USERNAME.in(
                                select(RECIPIENTS.USERNAME).from(RECIPIENTS)
                                        .join(RECIPIENT_NOTIFICATIONS)
                                        .on(RECIPIENT_NOTIFICATIONS.USERNAME.eq(RECIPIENTS.USERNAME))
                                        .where(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.eq(type))
                                        .and(RECIPIENT_NOTIFICATIONS.ACTIVE.eq(true))
                                        .and(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.isNull()
                                                .or((RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.plus(RECIPIENT_NOTIFICATIONS.FREQUENCY)
                                                        .lessThan(date))))
                        ))
        );
    }

    private Mono<RecipientsRecord> insertRecipientSql(Recipient recipient) {
        return wrapper.withDSLContext(ctx ->
                ctx.insertInto(RECIPIENTS)
                        .columns(RECIPIENTS.USERNAME, RECIPIENTS.EMAIL)
                        .values(recipient.getUsername(), recipient.getEmail())
                        .returning()
        );
    }

    private Mono<RecipientsRecord> updateRecipientSql(Recipient recipient) {
        return wrapper.withDSLContext(ctx ->
                ctx.update(RECIPIENTS)
                        .set(RECIPIENTS.EMAIL, recipient.getEmail())
                        .where(RECIPIENTS.USERNAME.eq(recipient.getUsername()))
                        .returning()
        );
    }

    private Mono<Map<NotificationType, NotificationSettings>> insertNotifications(Recipient recipient) {
        return Flux.fromIterable(recipient.getNotifications().entrySet())
                .flatMap(entry -> wrapper.withDSLContext(ctx ->
                        ctx.insertInto(RECIPIENT_NOTIFICATIONS)
                                .columns(
                                        RECIPIENT_NOTIFICATIONS.USERNAME,
                                        RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE,
                                        RECIPIENT_NOTIFICATIONS.ACTIVE,
                                        RECIPIENT_NOTIFICATIONS.FREQUENCY,
                                        RECIPIENT_NOTIFICATIONS.NOTIFY_DATE
                                ).values(
                                        recipient.getUsername(),
                                        entry.getKey(),
                                        entry.getValue().isActive(),
                                        entry.getValue().getFrequency().getKey(),
                                        entry.getValue().getNotifyDate()
                                ).returning()
                )).map(record ->
                        Maps.immutableEntry(
                                record.getNotificationType(),
                                toNotificationSettings(record)
                        )
                ).collectList()
                .map(ImmutableMap::copyOf);
    }

    private Mono<Integer> deleteNotificationsSql(String username) {
        return wrapper.withDSLContext(ctx ->
                ctx.deleteFrom(RECIPIENT_NOTIFICATIONS)
                        .where(RECIPIENT_NOTIFICATIONS.USERNAME.eq(username))
        );
    }

    private Recipient toRecipient(List<Record> records) {
        if (records.isEmpty()) {
            return null;
        }

        var notifications = records.stream()
                .filter(r -> r.get(RECIPIENT_NOTIFICATIONS.USERNAME) != null)
                .collect(toImmutableMap(
                        r -> r.get(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE),
                        this::toNotificationSettings
                ));

        var record = records.get(0);
        return Recipient.builder()
                .username(record.get(RECIPIENTS.USERNAME))
                .email(record.get(RECIPIENTS.EMAIL))
                .notifications(notifications)
                .build();
    }

    private NotificationSettings toNotificationSettings(Record record) {
        return NotificationSettings.builder()
                .active(record.get(RECIPIENT_NOTIFICATIONS.ACTIVE))
                .frequency(Frequency.valueOf(record.get(RECIPIENT_NOTIFICATIONS.FREQUENCY)))
                .notifyDate(record.get(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE))
                .build();
    }
}
