package com.starvoicelanka.user.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.PagedResponse;
import com.starvoicelanka.user.dto.UserDtos;
import com.starvoicelanka.user.entity.AuditAction;
import com.starvoicelanka.user.entity.AuditLog;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import com.starvoicelanka.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userService.findByEmail(userDetails.getUsername()).orElse(null);
    }

    // UM01
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody UserDtos.RegisterRequest req) {
        User user = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                Map.of("user", UserDtos.UserProfileDto.from(user),
                        "message", "Account created. Check your messages for the 6-digit verification code.")
        ));
    }

    // UM03
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verify(@Valid @RequestBody UserDtos.VerifyRequest req) {
        var result = userService.verifyAccount(req.getEmail(), req.getCode());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("user", UserDtos.UserProfileDto.from(result.user()),
                        "message", result.alreadyVerified() ? "This account was already verified" : "Account verified, you can vote now")
        ));
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendVerification(@Valid @RequestBody UserDtos.ResendVerificationRequest req) {
        userService.resendVerification(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "A new verification code is on its way")));
    }

    // UM02
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody UserDtos.LoginRequest req) {
        User user = userService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("user", UserDtos.UserProfileDto.from(user),
                        "message", "Logged in successfully")
        ));
    }

    // UM05
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody UserDtos.ForgotPasswordRequest req) {
        userService.requestPasswordReset(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "If that email is registered, a reset code has been sent")));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody UserDtos.ResetPasswordRequest req) {
        userService.confirmPasswordReset(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password reset, you can log in now")));
    }

    // UM04
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> me(@AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.ok(UserDtos.UserProfileDto.from(user)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> updateMe(@AuthenticationPrincipal UserDetails principal,
                                                                         @RequestBody UserDtos.UpdateProfileRequest req) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        User updated = userService.updateProfile(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(UserDtos.UserProfileDto.from(updated)));
    }

    // UM06 - Admin endpoints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserDtos.UserProfileDto>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Page<User> userPage = userService.listUsers(PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending()), role, status, search);

        List<UserDtos.UserProfileDto> dtos = userPage.getContent().stream().map(UserDtos.UserProfileDto::from).toList();
        return ResponseEntity.ok(new PagedResponse<>(dtos, page, pageSize, userPage.getTotalElements()));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserDtos.AuditLogDto>> listAudit(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action) {

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Page<AuditLog> auditPage = userService.listAuditLog(userId, action, PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending()));

        List<UserDtos.AuditLogDto> dtos = auditPage.getContent().stream().map(UserDtos.AuditLogDto::from).toList();
        return ResponseEntity.ok(new PagedResponse<>(dtos, page, pageSize, auditPage.getTotalElements()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> setStatus(
            @PathVariable Long id,
            @RequestBody UserDtos.SetStatusRequest req,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User actor = getAuthenticatedUser(principal);
        User updated = userService.setStatus(id, req.getStatus(), actor, req.getReason(), request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(UserDtos.UserProfileDto.from(updated)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.ChangeRoleRequest req,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User actor = getAuthenticatedUser(principal);
        User updated = userService.changeRole(id, req.getRole(), actor, req.getReason(), request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(UserDtos.UserProfileDto.from(updated)));
    }

    // UM06 - Admin: permanently delete a user account (matches the "Delete" action
    // in the admin web UI). This performs a real hard delete, cleaning up every
    // dependent record (votes, payments, notifications, wallet, sponsor links)
    // first. For a user closing their OWN account, see AppWebController, which
    // uses the separate soft-close/anonymise flow instead - that endpoint is
    // intentionally not exposed here to avoid the API and the admin UI disagreeing
    // about what "delete" means.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User actor = getAuthenticatedUser(principal);
        userService.deleteUser(id, actor, request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "User account permanently deleted")));
    }
}
