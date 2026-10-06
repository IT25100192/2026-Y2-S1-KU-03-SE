package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Email delivery through SendGrid (stub - logs the email). */
public class SendGridEmailProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);

    @Override
    public void send(Notification notification, User recipient) {
        log.info("[sendgrid] Sent email to {}: {} | {}",
                recipient != null ? recipient.getEmail() : "unknown", notification.getSubject(), notification.getBody());
    }
}
