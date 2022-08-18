package com.github.galleog.piggymetrics.notification.service;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Service to sent email notifications scheduled using cron-like expressions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    @VisibleForTesting
    static final String ACCOUNT_SERVICE = "account-service";

    private final RecipientRepository recipientRepository;
    private final EmailService emailService;

    @VisibleForTesting
    @GrpcClient(ACCOUNT_SERVICE)
    ReactorAccountServiceGrpc.ReactorAccountServiceStub accountServiceStub;

    /**
     * Sends backup notifications to recipients that should be notified according to their notification settings.
     * <p/>
     * Recipient's current account data are sent as an email attachment.
     */
    @Scheduled(cron = "${backup.cron}")
    @SchedulerLock(name = "backupNotifications")
    public void sendBackupNotifications() {
        recipientRepository.readyToNotify(NotificationType.BACKUP, LocalDate.now())
                .flatMap(recipient ->
                        accountServiceStub.getAccount(
                                        AccountServiceProto.GetAccountRequest.newBuilder()
                                                .setName(recipient.getUsername())
                                                .build()
                                ).doOnNext(account -> {
                                    try {
                                        emailService.send(NotificationType.BACKUP, recipient,
                                                JsonFormat.printer().print(account));
                                    } catch (Exception e) {
                                        throw Exceptions.propagate(e);
                                    }
                                }).doOnError(e ->
                                        logger.error("Backup notification for user '" + recipient.getUsername() + "' failed", e))
                                .onErrorResume(e -> Mono.empty())
                                .flatMap(account -> recipientRepository.update(recipient.markNotified(NotificationType.BACKUP)))
                ).count()
                .subscribe(count -> logger.info("Backup notification sent to {} recipients", count));
    }

    /**
     * Sends reminder notifications to recipients that should be notified according to their notification settings.
     */
    @Scheduled(cron = "${remind.cron}")
    @SchedulerLock(name = "remindNotifications")
    public void sendRemindNotifications() {
        recipientRepository.readyToNotify(NotificationType.REMIND, LocalDate.now())
                .flatMap(recipient ->
                        Mono.just(recipient)
                                .doOnNext(r -> {
                                    try {
                                        emailService.send(NotificationType.REMIND, r, null);
                                    } catch (Exception e) {
                                        throw Exceptions.propagate(e);
                                    }
                                }).doOnError(e ->
                                        logger.error("Reminder notification for user '" + recipient.getUsername() + "' failed", e))
                                .onErrorResume(e -> Mono.empty())
                ).flatMap(recipient -> recipientRepository.update(recipient.markNotified(NotificationType.REMIND)))
                .count()
                .subscribe(count -> logger.info("Reminder notification sent to {} recipients", count));
    }
}
