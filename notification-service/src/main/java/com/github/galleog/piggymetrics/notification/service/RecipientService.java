package com.github.galleog.piggymetrics.notification.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.grpc.ReactorRecipientServiceGrpc;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto.GetRecipientRequest;
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

import java.time.DateTimeException;
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

    private final RecipientRepository recipientRepository;

    @Override
    public Mono<RecipientServiceProto.Recipient> getRecipient(Mono<GetRecipientRequest> request) {
        return request.map(GetRecipientRequest::getUserName)
                .flatMap(this::doGetRecipient)
                .map(RECIPIENT_CONVERTER::convert);
    }

    @Override
    @Transactional
    public Mono<RecipientServiceProto.Recipient> updateRecipient(Mono<RecipientServiceProto.Recipient> request) {
        return request.map(recipient -> RECIPIENT_CONVERTER.reverse().convert(recipient))
                .flatMap(this::doUpdateRecipient)
                .map(RECIPIENT_CONVERTER::convert);
    }

    private Mono<Recipient> doGetRecipient(String name) {
        return recipientRepository.getByUsername(name)
                .doOnNext(recipient -> logger.debug("Notifications for user '{}' found", name))
                .switchIfEmpty(Mono.error(() -> Status.NOT_FOUND
                        .withDescription("Notifications for user '" + name + "' not found")
                        .asRuntimeException()));
    }

    private Mono<Recipient> doUpdateRecipient(Recipient recipient) {
        return recipientRepository.update(recipient)
                .doOnNext(r -> logger.info("Notification settings for user '{}' updated", r.getUsername()))
                .switchIfEmpty(Mono.defer(() ->
                        recipientRepository.save(recipient)
                                .doOnNext(r -> logger.info("Notification settings for user '{}' created", r.getUsername()))
                ));
    }

    private static final class RecipientConverter extends Converter<Recipient, RecipientServiceProto.Recipient> {
        @Override
        @NonNull
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
        @NonNull
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
        @NonNull
        protected RecipientServiceProto.NotificationSettings doForward(@NonNull NotificationSettings notificationSettings) {
            var builder = RecipientServiceProto.NotificationSettings.newBuilder()
                    .setActive(notificationSettings.isActive())
                    .setFrequency(notificationSettings.getFrequency().getKey());
            if (notificationSettings.getNotifyDate() != null) {
                builder.setNotifyDate(dateConverter().convert(notificationSettings.getNotifyDate()));
            }
            return builder.build();
        }

        @Override
        @NonNull
        protected NotificationSettings doBackward(@NonNull RecipientServiceProto.NotificationSettings notificationSettings) {
            var builder = NotificationSettings.builder()
                    .active(notificationSettings.getActive())
                    .frequency(Frequency.valueOf(notificationSettings.getFrequency()));
            if (notificationSettings.hasNotifyDate()) {
                builder.notifyDate(dateConverter().reverse().convert(notificationSettings.getNotifyDate()));
            }
            return builder.build();
        }
    }
}
