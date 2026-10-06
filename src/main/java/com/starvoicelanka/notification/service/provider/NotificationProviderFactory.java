package com.starvoicelanka.notification.service.provider;

import com.starvoicelanka.notification.entity.NotificationChannel;

/**
 * FACTORY PATTERN - Notification module.
 *
 * Like VehicleFactory in the lecture: NotificationService asks for a provider
 * (using the configured provider name and the channel) and gets back a
 * NotificationProvider. It never uses "new SendGridEmailProvider()" itself,
 * so the big if-else that used to be inside deliver() is gone.
 */
public class NotificationProviderFactory {

    public NotificationProvider createProvider(String providerName, NotificationChannel channel) {
        if (channel == NotificationChannel.IN_APP) {
            return new InAppProvider();
        }
        if ("sendgrid".equalsIgnoreCase(providerName) && channel == NotificationChannel.EMAIL) {
            return new SendGridEmailProvider();
        }
        if ("twilio".equalsIgnoreCase(providerName) && channel == NotificationChannel.SMS) {
            return new TwilioSmsProvider();
        }
        // "console" and any other combination
        return new ConsoleProvider();
    }
}
