package com.github.galleog.piggymetrics.notification.service;

import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to sent email notifications scheduled using cron-like expressions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    @VisibleForTesting
    static final String ACCOUNT_SERVICE = "account-service";

    private final Scheduler jdbcScheduler;
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
        List<Recipient> recipients = recipientRepository.readyToNotify(NotificationType.BACKUP, LocalDate.now());
        Flux.fromIterable(recipients)
                .flatMap(recipient -> accountServiceStub.getAccount(
                        AccountServiceProto.GetAccountRequest.newBuilder()
                                .setName(recipient.getUsername())
                                .build())
                        .publishOn(jdbcScheduler)
                        .doOnNext(account -> {
                            try {
                                emailService.send(NotificationType.BACKUP, recipient, JsonFormat.printer().print(account));
                                recipient.markNotified(NotificationType.BACKUP);
                                recipientRepository.update(recipient);
                            } catch (Exception e) {
                                throw Exceptions.propagate(e);
                            }
                        }).onErrorContinue((e, account) ->
                                logger.error("Backup notification for user '" + recipient.getUsername() + "' failed", e)
                        )
                ).count()
                .subscribe(count -> logger.info("Backup notification sent to {} recipients", count));
    }

    /**
     * Sends reminder notifications to recipients that should be notified according to their notification settings.
     */
    @Scheduled(cron = "${remind.cron}")
    @SchedulerLock(name = "remindNotifications")
    public void sendRemindNotifications() {
        List<Recipient> recipients = recipientRepository.readyToNotify(NotificationType.REMIND, LocalDate.now());
        Flux.fromIterable(recipients)
                .publishOn(jdbcScheduler)
                .doOnNext(recipient -> {
                    try {
                        emailService.send(NotificationType.REMIND, recipient, null);
                        recipient.markNotified(NotificationType.REMIND);
                        recipientRepository.update(recipient);
                    } catch (Exception e) {
                        logger.error("Reminder notification for user '" + recipient.getUsername() + "' failed", e);
                    }
                }).count()
                .subscribe(count -> logger.info("Reminder notification sent to {} recipients", count));
    }
}
