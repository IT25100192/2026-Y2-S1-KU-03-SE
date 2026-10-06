package com.starvoicelanka.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_status_role", columnList = "status, role")
})
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 25)
    private String mobile;

    @Column(length = 30)
    private String nic;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.VOTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING;

    // UM03 - account verification
    @JsonIgnore
    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    @JsonIgnore
    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // UM05 - password reset
    @JsonIgnore
    @Column(name = "reset_code", length = 10)
    private String resetCode;

    @JsonIgnore
    @Column(name = "reset_expires_at")
    private LocalDateTime resetExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // UM06 - account closure
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "anonymised_at")
    private LocalDateTime anonymisedAt;

    public User() {}

    public User(String fullName, String email, String mobile, Role role) {
        this(fullName, email, mobile, null, null, role, UserStatus.PENDING);
    }

    public User(String fullName, String email, String mobile, String nic, String passwordHash, Role role, UserStatus status) {
        this.fullName = fullName;
        this.email = email != null ? email.toLowerCase().trim() : null;
        this.mobile = mobile != null ? mobile.trim() : null;
        this.nic = nic != null ? nic.toUpperCase().trim() : null;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.VOTER;
        this.status = status != null ? status : UserStatus.PENDING;
    }

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.toLowerCase().trim() : null; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile != null ? mobile.trim() : null; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic != null ? nic.toUpperCase().trim() : null; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public LocalDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
    public void setVerificationExpiresAt(LocalDateTime verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }

    public LocalDateTime getResetExpiresAt() { return resetExpiresAt; }
    public void setResetExpiresAt(LocalDateTime resetExpiresAt) { this.resetExpiresAt = resetExpiresAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getAnonymisedAt() { return anonymisedAt; }
    public void setAnonymisedAt(LocalDateTime anonymisedAt) { this.anonymisedAt = anonymisedAt; }

    public boolean isMobileVerified() {
        return status == UserStatus.ACTIVE && verifiedAt != null;
    }
}
