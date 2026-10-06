package com.starvoicelanka.user.dto;

import com.starvoicelanka.user.entity.AuditAction;
import com.starvoicelanka.user.entity.AuditLog;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserDtos {

    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 120, message = "Full name must be between 3 and 120 characters")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        @NotBlank(message = "Mobile number is required")
        private String mobile;

        private String nic;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getNic() { return nic; }
        public void setNic(String nic) { this.nic = nic; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class VerifyRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Code must be 6 digits")
        private String code;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ResendVerificationRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Code is required")
        @Size(min = 6, max = 6, message = "Code must be 6 digits")
        private String code;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class UpdateProfileRequest {
        private String fullName;
        private String mobile;
        private String nic;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getNic() { return nic; }
        public void setNic(String nic) { this.nic = nic; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ChangeRoleRequest {
        private Role role;

        @NotBlank(message = "Reason is required")
        @Size(min = 4, message = "Reason must be at least 4 characters")
        private String reason;

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SetStatusRequest {
        private UserStatus status;
        private String reason;

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CloseAccountRequest {
        private String password;
        private String reason;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class UserProfileDto {
        private Long id;
        private String fullName;
        private String email;
        private String mobile;
        private String nic;
        private Role role;
        private UserStatus status;
        private LocalDateTime verifiedAt;
        private LocalDateTime lastLoginAt;
        private LocalDateTime closedAt;
        private LocalDateTime createdAt;

        public static UserProfileDto from(User u) {
            if (u == null) return null;
            UserProfileDto dto = new UserProfileDto();
            dto.id = u.getId();
            dto.fullName = u.getFullName();
            dto.email = u.getEmail();
            dto.mobile = u.getMobile();
            dto.nic = u.getNic();
            dto.role = u.getRole();
            dto.status = u.getStatus();
            dto.verifiedAt = u.getVerifiedAt();
            dto.lastLoginAt = u.getLastLoginAt();
            dto.closedAt = u.getClosedAt();
            dto.createdAt = u.getCreatedAt();
            return dto;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getMobile() { return mobile; }
        public String getNic() { return nic; }
        public Role getRole() { return role; }
        public UserStatus getStatus() { return status; }
        public LocalDateTime getVerifiedAt() { return verifiedAt; }
        public LocalDateTime getLastLoginAt() { return lastLoginAt; }
        public LocalDateTime getClosedAt() { return closedAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class AuditLogDto {
        private Long id;
        private AuditAction action;
        private Long actorId;
        private String actorEmail;
        private Long targetUserId;
        private String targetEmail;
        private String beforeVal;
        private String afterVal;
        private String reason;
        private String ipAddress;
        private LocalDateTime createdAt;

        public static AuditLogDto from(AuditLog a) {
            if (a == null) return null;
            AuditLogDto dto = new AuditLogDto();
            dto.id = a.getId();
            dto.action = a.getAction();
            dto.actorId = a.getActor() != null ? a.getActor().getId() : null;
            dto.actorEmail = a.getActorEmail();
            dto.targetUserId = a.getTargetUser() != null ? a.getTargetUser().getId() : null;
            dto.targetEmail = a.getTargetEmail();
            dto.beforeVal = a.getBeforeVal();
            dto.afterVal = a.getAfterVal();
            dto.reason = a.getReason();
            dto.ipAddress = a.getIpAddress();
            dto.createdAt = a.getCreatedAt();
            return dto;
        }

        public Long getId() { return id; }
        public AuditAction getAction() { return action; }
        public Long getActorId() { return actorId; }
        public String getActorEmail() { return actorEmail; }
        public Long getTargetUserId() { return targetUserId; }
        public String getTargetEmail() { return targetEmail; }
        public String getBeforeVal() { return beforeVal; }
        public String getAfterVal() { return afterVal; }
        public String getReason() { return reason; }
        public String getIpAddress() { return ipAddress; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
