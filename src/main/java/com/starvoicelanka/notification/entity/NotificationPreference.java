package com.starvoicelanka.notification.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences", indexes = {
        @Index(name = "idx_notify_pref_user", columnList = "user_id", unique = true)
})
public class NotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean email = true;

    @Column(nullable = false)
    private boolean sms = true;

    @Column(name = "in_app", nullable = false)
    private boolean inApp = true;

    @Column(nullable = false)
    private boolean announcements = true;

    public NotificationPreference() {}

    public NotificationPreference(User user) {
        this.user = user;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isEmail() { return email; }
    public void setEmail(boolean email) { this.email = email; }

    public boolean isSms() { return sms; }
    public void setSms(boolean sms) { this.sms = sms; }

    public boolean isInApp() { return inApp; }
    public void setInApp(boolean inApp) { this.inApp = inApp; }

    public boolean isAnnouncements() { return announcements; }
    public void setAnnouncements(boolean announcements) { this.announcements = announcements; }
}
