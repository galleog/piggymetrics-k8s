package com.github.galleog.sk8s.notification.service;

import com.github.galleog.sk8s.notification.domain.NotificationType;
import com.github.galleog.sk8s.notification.domain.Recipient;

import javax.mail.MessagingException;
import java.io.IOException;

public interface EmailService {

    void send(NotificationType type, Recipient recipient, String attachment) throws MessagingException, IOException;

}
