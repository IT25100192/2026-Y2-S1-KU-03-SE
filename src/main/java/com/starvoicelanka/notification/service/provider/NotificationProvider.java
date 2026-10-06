package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.user.entity.User;

/**
 * Common interface for every way of delivering a notification
 * (the "Vehicle" interface of the Factory pattern example in the lecture).
 */
public interface NotificationProvider {

    /** Deliver the message. Throw an exception if delivery fails so the retry logic can pick it up. */
    void send(Notification notification, User recipient);
}
