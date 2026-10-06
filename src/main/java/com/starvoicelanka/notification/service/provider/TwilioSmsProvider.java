package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SMS delivery through Twilio (stub - logs the SMS). */
public class TwilioSmsProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

    @Override
    public void send(Notification notification, User recipient) {
        log.info("[twilio] Sent SMS to {}: {}", recipient != null ? recipient.getMobile() : "unknown", notification.getBody());
    }
}
