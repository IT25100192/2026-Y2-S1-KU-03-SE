package com.starvoicelanka.notification.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notify_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_notify_status_next", columnList = "status, next_attempt_at")
})
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "abandoned_at")
    private LocalDateTime abandonedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean broadcast = false;

    public Notification() {}

    public Notification(User user, NotificationTemplate template, NotificationChannel channel, String subject, String body, boolean broadcast) {
        this.user = user;
        this.template = template;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.broadcast = broadcast;
        this.status = NotificationStatus.QUEUED;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public NotificationTemplate getTemplate() { return template; }
    public void setTemplate(NotificationTemplate template) { this.template = template; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public LocalDateTime getAbandonedAt() { return abandonedAt; }
    public void setAbandonedAt(LocalDateTime abandonedAt) { this.abandonedAt = abandonedAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public boolean isBroadcast() { return broadcast; }
    public void setBroadcast(boolean broadcast) { this.broadcast = broadcast; }
}
