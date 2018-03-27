package com.github.galleog.sk.notification.service;

import com.github.galleog.sk.notification.domain.NotificationType;
import com.github.galleog.sk.notification.domain.Recipient;

import javax.mail.MessagingException;
import java.io.IOException;

public interface EmailService {

    void send(NotificationType type, Recipient recipient, String attachment) throws MessagingException, IOException;

}
