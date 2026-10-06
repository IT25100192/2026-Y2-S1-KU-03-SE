package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.user.entity.User;

/** In-app notifications are only stored in the database, so there is nothing to send. */
public class InAppProvider implements NotificationProvider {

    @Override
    public void send(Notification notification, User recipient) {
        // stored in DB by NotificationService
    }
}
