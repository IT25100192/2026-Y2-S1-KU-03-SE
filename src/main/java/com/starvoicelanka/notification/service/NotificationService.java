package com.starvoicelanka.notification.service;

import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.notification.entity.NotificationChannel;
import com.starvoicelanka.notification.entity.NotificationPreference;
import com.starvoicelanka.notification.entity.NotificationStatus;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.repository.NotificationPreferenceRepository;
import com.starvoicelanka.notification.repository.NotificationRepository;
import com.starvoicelanka.notification.service.provider.NotificationProvider;
import com.starvoicelanka.notification.service.provider.NotificationProviderFactory;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationTemplateRenderer renderer;
    private final AppProperties properties;
    private final NotificationProviderFactory providerFactory = new NotificationProviderFactory();

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               UserRepository userRepository,
                               NotificationTemplateRenderer renderer,
                               AppProperties properties) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.renderer = renderer;
        this.properties = properties;
    }

    public record RetryResult(int picked, int sent, int stillFailing) {}
    public record BroadcastResult(int recipients, int messages) {}

    public NotificationPreference getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    NotificationPreference pref = new NotificationPreference(user);
                    return preferenceRepository.save(pref);
                });
    }

    public NotificationPreference updatePreferences(Long userId, Boolean email, Boolean sms, Boolean inApp, Boolean announcements) {
        NotificationPreference prefs = getPreferences(userId);
        if (email != null) prefs.setEmail(email);
        if (sms != null) prefs.setSms(sms);
        if (inApp != null) prefs.setInApp(inApp);
        if (announcements != null) prefs.setAnnouncements(announcements);
        return preferenceRepository.save(prefs);
    }

    private boolean isChannelAllowed(NotificationPreference prefs, NotificationChannel channel, boolean isBroadcast) {
        if (isBroadcast && !prefs.isAnnouncements()) return false;
        return switch (channel) {
            case EMAIL -> prefs.isEmail();
            case SMS -> prefs.isSms();
            case IN_APP -> prefs.isInApp();
        };
    }

    public LocalDateTime calculateBackoff(int attempts) {
        long minutes = (long) Math.pow(5, Math.max(0, attempts - 1));
        return LocalDateTime.now().plusMinutes(minutes);
    }

    public List<Notification> dispatch(Long userId, NotificationTemplate template, Map<String, Object> payload) {
        return dispatch(userId, template, payload, false, null);
    }

    public List<Notification> dispatch(Long userId, NotificationTemplate template, Map<String, Object> payload, boolean broadcast) {
        return dispatch(userId, template, payload, broadcast, null);
    }

    public List<Notification> dispatch(Long userId, NotificationTemplate template, Map<String, Object> payload,
                                       boolean broadcast, List<NotificationChannel> overrideChannels) {
        NotificationTemplateRenderer.RenderedMessage rendered = renderer.render(template, payload);
        List<NotificationChannel> wanted = overrideChannels != null ? overrideChannels : rendered.channels();

        User recipient = userId != null ? userRepository.findById(userId).orElse(null) : null;
        NotificationPreference prefs = (userId != null && recipient != null) ? getPreferences(userId) : null;

        List<NotificationChannel> finalChannels = new ArrayList<>();
        for (NotificationChannel ch : wanted) {
            if (prefs == null || isChannelAllowed(prefs, ch, broadcast)) {
                finalChannels.add(ch);
            }
        }

        // Critical templates (Account verification & Password reset) always send via at least EMAIL if empty
        boolean isCritical = (template == NotificationTemplate.ACCOUNT_VERIFICATION || template == NotificationTemplate.PASSWORD_RESET);
        if (finalChannels.isEmpty() && isCritical) {
            finalChannels.add(NotificationChannel.EMAIL);
        }

        List<Notification> createdList = new ArrayList<>();
        for (NotificationChannel channel : finalChannels) {
            Notification notification = new Notification(recipient, template, channel, rendered.subject(), rendered.body(), broadcast);
            deliverAndTrack(notification, recipient);
            createdList.add(notificationRepository.save(notification));
        }

        return createdList;
    }

    private void deliverAndTrack(Notification notification, User recipient) {
        notification.setAttempts(notification.getAttempts() + 1);
        try {
            deliver(notification.getChannel(), notification, recipient);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setLastError(null);
            notification.setNextAttemptAt(null);
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError(ex.getMessage());
            notification.setNextAttemptAt(calculateBackoff(notification.getAttempts()));
            if (notification.getAttempts() >= properties.getNotifyMaxAttempts()) {
                notification.setAbandonedAt(LocalDateTime.now());
                notification.setNextAttemptAt(null);
            }
        }
    }

    /** FACTORY PATTERN: the factory chooses the provider, this method just uses it. */
    private void deliver(NotificationChannel channel, Notification notification, User recipient) {
        NotificationProvider provider = providerFactory.createProvider(properties.getNotifyProvider(), channel);
        provider.send(notification, recipient);
    }

    public BroadcastResult broadcast(List<Long> recipientIds, NotificationTemplate template, Map<String, Object> payload) {
        int messageCount = 0;
        for (Long uid : recipientIds) {
            List<Notification> sent = dispatch(uid, template, payload, true, null);
            messageCount += sent.size();
        }
        return new BroadcastResult(recipientIds.size(), messageCount);
    }

    public BroadcastResult broadcast(NotificationTemplate template, Map<String, Object> payload) {
        List<Long> allUserIds = userRepository.findAll().stream().map(User::getId).toList();
        return broadcast(allUserIds, template, payload);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listForUser(Long userId, Pageable pageable) {
        return listForUser(userId, pageable, false);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listForUser(Long userId, Pageable pageable, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP, pageable);
        }
        return notificationRepository.findByUserIdAndChannel(userId, NotificationChannel.IN_APP, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP);
    }

    public Notification markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notification;
    }

    public int markAllRead(Long userId) {
        return notificationRepository.markAllAsReadForUser(userId, LocalDateTime.now());
    }

    public RetryResult retryFailed(int maxAttempts, int batchSize) {
        Pageable limit = PageRequest.of(0, batchSize, Sort.by("nextAttemptAt").ascending());
        List<Notification> due = notificationRepository.findDueForRetry(NotificationStatus.FAILED, maxAttempts, LocalDateTime.now(), limit);

        int sent = 0;
        int stillFailing = 0;

        for (Notification n : due) {
            deliverAndTrack(n, n.getUser());
            notificationRepository.save(n);
            if (n.getStatus() == NotificationStatus.SENT) {
                sent++;
            } else {
                stillFailing++;
            }
        }

        return new RetryResult(due.size(), sent, stillFailing);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listFailed(Pageable pageable) {
        return listFailed(pageable, false);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listFailed(Pageable pageable, boolean includeAbandoned) {
        if (includeAbandoned) {
            return notificationRepository.findByStatus(NotificationStatus.FAILED, pageable);
        }
        return notificationRepository.findByStatusAndAbandonedAtIsNull(NotificationStatus.FAILED, pageable);
    }

    public Notification requeue(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        n.setStatus(NotificationStatus.FAILED);
        n.setAbandonedAt(null);
        n.setAttempts(0);
        n.setNextAttemptAt(null);
        return notificationRepository.save(n);
    }

    /* ------------------------------------------------------------------ */
    /* Full CRUD Operations for Admin Notification Management              */
    /* ------------------------------------------------------------------ */

    // 1. CREATE
    public Notification createNotification(Long recipientUserId, NotificationChannel channel, String subject, String body) {
        subject = InputValidator.requireText(subject, "Subject", 3, 255);
        body = InputValidator.requireText(body, "Message", 3, 2000);
        User user = null;
        if (recipientUserId != null) {
            user = userRepository.findById(recipientUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("No user with id " + recipientUserId + " to send this to"));
        }
        Notification notification = new Notification(
                user,
                NotificationTemplate.GENERAL_ANNOUNCEMENT,
                channel != null ? channel : NotificationChannel.IN_APP,
                subject != null && !subject.isBlank() ? subject : "StarVoice Lanka Announcement",
                body != null ? body : "",
                user == null
        );
        deliverAndTrack(notification, user);
        return notificationRepository.save(notification);
    }

    // 2. READ (List, filter, search)
    @Transactional(readOnly = true)
    public Page<Notification> listAll(NotificationChannel channel, NotificationStatus status, String search, Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(root.get("body")), pattern),
                        cb.like(cb.lower(root.join("user", JoinType.LEFT).get("email")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return notificationRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Notification getNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    // 3. UPDATE
    public Notification updateNotification(Long id, String subject, String body, NotificationStatus status) {
        Notification notification = getNotification(id);
        if (subject != null && !subject.isBlank()) {
            notification.setSubject(InputValidator.requireText(subject, "Subject", 3, 255));
        }
        if (body != null && !body.isBlank()) {
            notification.setBody(InputValidator.requireText(body, "Message", 3, 2000));
        }
        if (status != null) {
            notification.setStatus(status);
        }
        return notificationRepository.save(notification);
    }

    public Notification resend(Long id) {
        Notification notification = getNotification(id);
        deliverAndTrack(notification, notification.getUser());
        return notificationRepository.save(notification);
    }

    // 4. DELETE
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        notificationRepository.deleteById(id);
    }

    public int deleteFailedNotifications() {
        List<Notification> failed = notificationRepository.findByStatus(NotificationStatus.FAILED, Pageable.unpaged()).getContent();
        notificationRepository.deleteAll(failed);
        return failed.size();
    }
}
