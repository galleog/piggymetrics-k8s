package com.github.galleog.piggymetrics.gateway.handler;

import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;

import com.github.galleog.piggymetrics.gateway.model.notification.Frequency;
import com.github.galleog.piggymetrics.gateway.model.notification.NotificationSettings;
import com.github.galleog.piggymetrics.gateway.model.notification.NotificationType;
import com.github.galleog.piggymetrics.gateway.model.notification.Recipient;
import com.github.galleog.piggymetrics.notification.grpc.ReactorRecipientServiceGrpc;
import com.github.galleog.piggymetrics.notification.grpc.RecipientServiceProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Routing handler for recipients.
 */
@Slf4j
@Component
public class NotificationHandler {
    @VisibleForTesting
    static final String NOTIFICATION_SERVICE = "notification-service";

    private static final Converter<NotificationSettings, RecipientServiceProto.NotificationSettings>
            NOTIFICATION_SETTINGS_CONVERTER = new NotificationSettingsConverter();

    @GrpcClient(NOTIFICATION_SERVICE)
    private ReactorRecipientServiceGrpc.ReactorRecipientServiceStub recipientServiceStub;

    /**
     * Gets notification settings for the current principal.
     *
     * @param request the server request
     * @return the recipient corresponding to the current principal, or {@link HttpStatus#NOT_FOUND}
     * if there exists no recipient with the name of the current principal
     */
    public Mono<ServerResponse> getCurrentNotificationsSettings(ServerRequest request) {
        Mono<Recipient> mono = request.principal()
                .map(principal -> RecipientServiceProto.GetRecipientRequest.newBuilder()
                        .setUserName(principal.getName())
                        .build())
                .compose(recipientServiceStub::getRecipient)
                .map(this::toRecipient);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mono, Recipient.class);
    }

    /**
     * Saves notification settings of the current principal.
     *
     * @param request the server request
     * @return the saved recipient corresponding to the current principal}
     */
    public Mono<ServerResponse> updateCurrentNotificationsSettings(ServerRequest request) {
        Mono<Recipient> mono = Mono.zip(request.principal(), request.bodyToMono(Recipient.class))
                .map(tuple -> toRecipientProto(tuple.getT1().getName(), tuple.getT2()))
                .compose(recipientServiceStub::updateRecipient)
                .map(this::toRecipient);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mono, Recipient.class);
    }

    private RecipientServiceProto.Recipient toRecipientProto(String name, Recipient recipient) {
        return RecipientServiceProto.Recipient.newBuilder()
                .setUserName(name)
                .setEmail(recipient.getEmail())
                .putAllNotifications(
                        recipient.getNotifications().entrySet().stream()
                                .collect(Collectors.toMap(
                                        entry -> entry.getKey().name(),
                                        entry -> NOTIFICATION_SETTINGS_CONVERTER.convert(entry.getValue())
                                ))
                ).build();
    }

    private Recipient toRecipient(RecipientServiceProto.Recipient recipient) {
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
