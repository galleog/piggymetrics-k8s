package com.github.galleog.piggymetrics.notification.service;

import com.github.galleog.piggymetrics.notification.client.AccountServiceClient;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service to sent email notifications scheduled using cron-like expressions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final AccountServiceClient client;
    private final RecipientService recipientService;
    private final EmailService emailService;

    /**
     * Sends backup notifications to recipients that should be notified according to their notification settings.
     * <p/>
     * Recipient's current account data are sent as an email attachment.
     */
    @Scheduled(cron = "${backup.cron}")
    @SchedulerLock(name = "backupNotifications")
    public void sendBackupNotifications() {
        List<Recipient> recipients = recipientService.readyToNotify(NotificationType.BACKUP);
        logger.info("Found {} recipients for backup notification", recipients.size());

        recipients.forEach(recipient ->
                CompletableFuture.runAsync(() -> {
                    try {
                        String attachment = client.getAccount(recipient.getAccountName());
                        emailService.send(NotificationType.BACKUP, recipient, attachment);
                        recipientService.markNotified(NotificationType.BACKUP, recipient);
                    } catch (Exception e) {
                        logger.error("Backup notification for " + recipient.getAccountName() + " failed", e);
                    }
                })
        );
    }

    /**
     * Sends remind notifications to recipients that should be notified according to their notification settings.
     */
    @Scheduled(cron = "${remind.cron}")
    @SchedulerLock(name = "remindNotifications")
    public void sendRemindNotifications() {
        List<Recipient> recipients = recipientService.readyToNotify(NotificationType.REMIND);
        logger.info("Found {} recipients for remind notification", recipients.size());

        recipients.forEach(recipient ->
                CompletableFuture.runAsync(() -> {
                    try {
                        emailService.send(NotificationType.REMIND, recipient, null);
                        recipientService.markNotified(NotificationType.REMIND, recipient);
                    } catch (Exception e) {
                        logger.error("Remind notification for " + recipient.getAccountName() + " failed", e);
                    }
                })
        );
    }
}
