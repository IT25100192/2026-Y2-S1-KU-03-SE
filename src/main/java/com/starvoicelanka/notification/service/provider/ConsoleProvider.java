package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Development provider: prints the message to the log instead of sending it. Also the fallback. */
public class ConsoleProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(ConsoleProvider.class);

    @Override
    public void send(Notification notification, User recipient) {
        String target = recipient != null ? recipient.getEmail() : "ALL";
        log.info("[notify:{}] {} -> user {}: {}", notification.getChannel(), notification.getSubject(), target, notification.getBody());
    }
}
