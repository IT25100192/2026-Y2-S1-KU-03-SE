package com.starvoicelanka.user.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_target_created", columnList = "target_user_id, created_at")
})
public class AuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(name = "target_email", length = 180)
    private String targetEmail;

    @Column(name = "before_val", length = 255)
    private String beforeVal;

    @Column(name = "after_val", length = 255)
    private String afterVal;

    @Column(length = 500)
    private String reason;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    public AuditLog() {}

    public AuditLog(AuditAction action, User actor, String actorEmail, User targetUser, String targetEmail,
                    String beforeVal, String afterVal, String reason, String ipAddress) {
        this.action = action;
        this.actor = actor;
        this.actorEmail = actorEmail;
        this.targetUser = targetUser;
        this.targetEmail = targetEmail;
        this.beforeVal = beforeVal;
        this.afterVal = afterVal;
        this.reason = reason;
        this.ipAddress = ipAddress;
    }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }

    public String getTargetEmail() { return targetEmail; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }

    public String getBeforeVal() { return beforeVal; }
    public void setBeforeVal(String beforeVal) { this.beforeVal = beforeVal; }

    public String getStateBefore() { return beforeVal; }
    public void setStateBefore(String stateBefore) { this.beforeVal = stateBefore; }

    public String getAfterVal() { return afterVal; }
    public void setAfterVal(String afterVal) { this.afterVal = afterVal; }

    public String getStateAfter() { return afterVal; }
    public void setStateAfter(String stateAfter) { this.afterVal = stateAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
