package com.github.galleog.piggymetrics.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import lombok.Cleanup;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simplejavamail.converter.internal.mimemessage.MimeMessageParser;
import org.simplejavamail.converter.internal.mimemessage.MimeMessageParser.ParsedMimeMessageComponents;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * Tests for {@link EmailService}.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String SUBJECT = "subject";
    private static final String TEXT = "text";
    private static final String ATTACHMENT_FILENAME = "attachment.json";
    private static final String ATTACHMENT = "{\"name\":\"test\"}";

    @Mock
    private JavaMailSender mailSender;
    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private Recipient recipient;

    @BeforeEach
    void setUp() {
        recipient = Recipient.builder()
                .username(USERNAME)
                .email(EMAIL)
                .build();
    }

    /**
     * Test for {@link EmailService#send(NotificationType, Recipient, String)} when there is an attachment.
     */
    @Test
    void shouldSendEmailWithAttachment() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        EmailService emailService = new EmailService(mailSender, mockEnvironment());
        emailService.send(NotificationType.BACKUP, recipient, ATTACHMENT);
        verify(mailSender).send(messageCaptor.capture());

        ParsedMimeMessageComponents components = MimeMessageParser.parseMimeMessage(messageCaptor.getValue());
        assertThat(components.getSubject()).isEqualTo(SUBJECT);
        assertThat(components.getToAddresses()).extracting(Address::toString)
                .containsExactly(EMAIL);
        assertThat(components.getPlainContent()).isEqualTo(TEXT);

        Map<String, DataSource> attachments = components.getAttachmentList();
        assertThat(attachments).hasSize(1);

        @Cleanup InputStream is = attachments.get(ATTACHMENT_FILENAME).getInputStream();
        assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo(ATTACHMENT);
    }

    /**
     * Test for {@link EmailService#send(NotificationType, Recipient, String)} when no attachment is specified.
     */
    @Test
    void shouldSendEmailWithoutAttachmentIfNoAttachmentPassed() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        EmailService emailService = new EmailService(mailSender, mockEnvironment());
        emailService.send(NotificationType.BACKUP, recipient, null);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(MimeMessageParser.parseMimeMessage(messageCaptor.getValue()).getAttachmentList()).isEmpty();
    }

    /**
     * Test for {@link EmailService#send(NotificationType, Recipient, String)}
     * when the attachment filename isn't set but an attachment is passed.
     */
    @Test
    void shouldFailToSendEmailIfAttachmentFilenameEmptyButAttachmentPassed() {
        EmailService emailService = new EmailService(mailSender, mockEnvironment());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> emailService.send(NotificationType.REMIND, recipient, ATTACHMENT));
    }

    /**
     * Test for {@link EmailService#send(NotificationType, Recipient, String)}
     * when the attachment filename isn't set in the application properties but an attachment is passed.
     */
    @Test
    void shouldFailToSendEmailIfAttachmentFilenameNotSetButAttachmentPassed() {
        Environment env = new MockEnvironment()
                .withProperty(NotificationType.BACKUP.getSubject(), SUBJECT)
                .withProperty(NotificationType.BACKUP.getText(), TEXT);
        EmailService emailService = new EmailService(mailSender, env);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> emailService.send(NotificationType.BACKUP, recipient, ATTACHMENT));
    }

    private Environment mockEnvironment() {
        return new MockEnvironment()
                .withProperty(NotificationType.BACKUP.getSubject(), SUBJECT)
                .withProperty(NotificationType.BACKUP.getText(), TEXT)
                .withProperty(NotificationType.BACKUP.getAttachment(), ATTACHMENT_FILENAME)
                .withProperty(NotificationType.REMIND.getSubject(), SUBJECT)
                .withProperty(NotificationType.REMIND.getText(), TEXT);
    }
}