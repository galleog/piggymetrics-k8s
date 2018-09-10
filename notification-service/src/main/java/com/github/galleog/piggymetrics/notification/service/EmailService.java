package com.github.galleog.piggymetrics.notification.service;

import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;

/**
 * Service to send email notification to recipients.
 */
@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final Environment env;

    /**
     * Sends an email notification to a recipient.
     *
     * @param type       the notification type
     * @param recipient  the recipient to send the notification to
     * @param attachment the optional attachment to the message
     * @throws NullPointerException  if the notification type or recipient is {@code null}
     * @throws IllegalStateException if the attachment isn't blank but the notification type can't have an attachment or
     *                               its filename isn't set in the application properties
     * @throws MessagingException    if the email notification can't be sent
     */
    public void send(@NonNull NotificationType type, @NonNull Recipient recipient, @Nullable String attachment)
            throws MessagingException {
        Validate.notNull(type);
        Validate.notNull(recipient);

        String filename = type.getAttachment() != null ? env.getProperty(type.getAttachment()) : null;
        Validate.validState(StringUtils.isNotBlank(filename) || StringUtils.isAllBlank(filename, attachment),
                "If an attachment is specified then its filename must not be blank");

        String subject = env.getRequiredProperty(type.getSubject());
        String text = MessageFormat.format(env.getRequiredProperty(type.getText()), recipient.getAccountName());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(recipient.getEmail());
        helper.setSubject(subject);
        helper.setText(text);

        if (StringUtils.isNotBlank(attachment)) {
            helper.addAttachment(filename, new ByteArrayResource(attachment.getBytes()));
        }

        mailSender.send(message);
        logger.info("Email notification of type {} has been send to {}", type, recipient.getEmail());
    }
}
