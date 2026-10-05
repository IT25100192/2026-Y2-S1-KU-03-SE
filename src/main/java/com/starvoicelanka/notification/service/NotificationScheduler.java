package com.starvoicelanka.notification.service;

import com.starvoicelanka.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final AppProperties properties;

    public NotificationScheduler(NotificationService notificationService, AppProperties properties) {
        this.notificationService = notificationService;
        this.properties = properties;
    }

    /**
     * NM06 - every five minutes.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void retryFailedNotifications() {
        if (!properties.isWorkersEnabled()) {
            return;
        }
        try {
            NotificationService.RetryResult result = notificationService.retryFailed(properties.getNotifyMaxAttempts(), 50);
            if (result.picked() > 0) {
                log.info("[worker:notify-retry] picked {}, sent {}, still failing {}",
                        result.picked(), result.sent(), result.stillFailing());
            }
        } catch (Exception ex) {
            log.error("[worker:notify-retry] failed: {}", ex.getMessage());
        }
    }
}
