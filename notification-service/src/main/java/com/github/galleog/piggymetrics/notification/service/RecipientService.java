package com.github.galleog.piggymetrics.notification.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.grpc.ReactorRecipientServiceGrpc;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.DateTimeException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Service for {@link Recipient}.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecipientService extends ReactorRecipientServiceGrpc.RecipientServiceImplBase {
    private static final Converter<NotificationSettings, RecipientServiceProto.NotificationSettings>
            NOTIFICATION_SETTINGS_CONVERTER = new NotificationSettingsConverter();
    private static final Converter<Recipient, RecipientServiceProto.Recipient> RECIPIENT_CONVERTER = new RecipientConverter();

    private final Scheduler jdbcScheduler;
    private final RecipientRepository repository;

    @Override
    public Mono<RecipientServiceProto.Recipient> getRecipient(Mono<RecipientServiceProto.GetRecipientRequest> request) {
        return request.flatMap(req ->
                async(() ->
                        repository.getByUsername(req.getUserName())
                                .orElseThrow(() -> Status.NOT_FOUND
                                        .withDescription("Notifications for user '" + req.getUserName() + "' not found")
                                        .asRuntimeException())
                ).doOnNext(recipient -> logger.debug("Notifications for user '{}' found", recipient))
        ).map(RECIPIENT_CONVERTER::convert);
    }

    @Override
    public Mono<RecipientServiceProto.Recipient> updateRecipient(Mono<RecipientServiceProto.Recipient> request) {
        return request.map(recipient -> RECIPIENT_CONVERTER.reverse().convert(recipient))
                .flatMap(recipient -> async(() -> doUpdateRecipient(recipient)))
                .map(RECIPIENT_CONVERTER::convert);
    }

    @Transactional
    private Recipient doUpdateRecipient(Recipient recipient) {
        Optional<Recipient> updated = repository.update(recipient);
        if (updated.isPresent()) {
            logger.info("Notification settings for user '{}' updated", recipient.getUsername());
            return updated.get();
        } else {
            repository.save(recipient);
            logger.info("Notification settings for user '{}' created", recipient.getUsername());
            return recipient;
        }
    }

    private <T> Mono<T> async(Callable<? extends T> supplier) {
        return Mono.<T>fromCallable(supplier)
                .subscribeOn(jdbcScheduler);
    }

    private static final class RecipientConverter extends Converter<Recipient, RecipientServiceProto.Recipient> {
        @Override
        protected RecipientServiceProto.Recipient doForward(@NonNull Recipient recipient) {
            return RecipientServiceProto.Recipient.newBuilder()
                    .setUserName(recipient.getUsername())
                    .setEmail(recipient.getEmail())
                    .putAllNotifications(
                            recipient.getNotifications().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            entry -> entry.getKey().name(),
                                            entry -> NOTIFICATION_SETTINGS_CONVERTER.convert(entry.getValue())
                                    ))
                    ).build();
        }

        @Override
        protected Recipient doBackward(@NonNull RecipientServiceProto.Recipient recipient) {
            try {
                return Recipient.builder()
                        .username(recipient.getUserName())
                        .email(recipient.getEmail())
                        .notifications(
                                recipient.getNotificationsMap().entrySet().stream()
                                        .collect(ImmutableMap.toImmutableMap(
                                                entry -> NotificationType.valueOf(entry.getKey()),
                                                entry -> NOTIFICATION_SETTINGS_CONVERTER.reverse().convert(entry.getValue())
                                        ))
                        ).build();
            } catch (NullPointerException | IllegalArgumentException | DateTimeException e) {
                throw Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .withCause(e)
                        .asRuntimeException();
            }
        }
    }

    private static final class NotificationSettingsConverter
            extends Converter<NotificationSettings, RecipientServiceProto.NotificationSettings> {
        @Override
        protected RecipientServiceProto.NotificationSettings doForward(@NonNull NotificationSettings notificationSettings) {
            RecipientServiceProto.NotificationSettings.Builder builder = RecipientServiceProto.NotificationSettings.newBuilder()
                    .setActive(notificationSettings.isActive())
                    .setFrequency(notificationSettings.getFrequency().getKey());
            if (notificationSettings.getNotifyDate() != null) {
                builder.setNotifyDate(dateConverter().convert(notificationSettings.getNotifyDate()));
            }
            return builder.build();
        }

        @Override
        protected NotificationSettings doBackward(@NonNull RecipientServiceProto.NotificationSettings notificationSettings) {
            NotificationSettings.NotificationSettingsBuilder builder = NotificationSettings.builder()
                    .active(notificationSettings.getActive())
                    .frequency(Frequency.valueOf(notificationSettings.getFrequency()));
            if (notificationSettings.hasNotifyDate()) {
                builder.notifyDate(dateConverter().reverse().convert(notificationSettings.getNotifyDate()));
            }
            return builder.build();
        }
    }
}
