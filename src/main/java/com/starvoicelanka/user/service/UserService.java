package com.starvoicelanka.user.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.common.exception.ForbiddenException;
import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.exception.UnauthorizedException;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.user.dto.UserDtos;
import com.starvoicelanka.user.entity.AuditAction;
import com.starvoicelanka.user.entity.AuditLog;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import com.starvoicelanka.user.repository.AuditLogRepository;
import com.starvoicelanka.user.repository.UserRepository;
import com.starvoicelanka.payment.repository.CreditAccountRepository;
import com.starvoicelanka.payment.repository.PaymentRepository;
import com.starvoicelanka.notification.repository.NotificationPreferenceRepository;
import com.starvoicelanka.notification.repository.NotificationRepository;
import com.starvoicelanka.voting.repository.VoteQuotaRepository;
import com.starvoicelanka.voting.repository.VoteRepository;
import com.starvoicelanka.sponsor.repository.SponsorRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    // FACTORY + STRATEGY: the factory returns the password rule that fits the role
    private final com.starvoicelanka.user.service.policy.PasswordPolicyFactory passwordPolicyFactory =
            new com.starvoicelanka.user.service.policy.PasswordPolicyFactory();

    @Autowired(required = false)
    private CreditAccountRepository creditAccountRepository;
    @Autowired(required = false)
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired(required = false)
    private NotificationRepository notificationRepository;
    @Autowired(required = false)
    private VoteQuotaRepository voteQuotaRepository;
    @Autowired(required = false)
    private VoteRepository voteRepository;
    @Autowired(required = false)
    private PaymentRepository paymentRepository;
    @Autowired(required = false)
    private SponsorRepository sponsorRepository;

    public UserService(UserRepository userRepository,
                       AuditLogRepository auditLogRepository,
                       NotificationService notificationService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    private String generateSixDigitCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public java.util.Optional<User> findByEmail(String email) {
        if (email == null) return java.util.Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public record VerifyResult(User user, boolean alreadyVerified) {}

    /**
     * UM01 - Register a Voter Account
     */
    public User register(UserDtos.RegisterRequest req) {
        String fullName = com.starvoicelanka.common.validation.InputValidator.requireText(req.getFullName(), "Full name", 3, 120);
        String email = com.starvoicelanka.common.validation.InputValidator.requireEmail(req.getEmail(), "Email");
        String mobile = com.starvoicelanka.common.validation.InputValidator.requireMobile(req.getMobile(), "Mobile number");
        String nic = com.starvoicelanka.common.validation.InputValidator.optionalNic(req.getNic());
        passwordPolicyFactory.createPolicy(Role.VOTER).validate(req.getPassword());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = new User(
                fullName,
                email,
                mobile,
                nic,
                passwordEncoder.encode(req.getPassword()),
                Role.VOTER,
                UserStatus.PENDING
        );

        user.setVerificationCode(generateSixDigitCode());
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        user = userRepository.save(user);

        notificationService.dispatch(user.getId(), NotificationTemplate.ACCOUNT_VERIFICATION, Map.of(
                "fullName", user.getFullName(),
                "code", user.getVerificationCode(),
                "expiresInMinutes", 15
        ));

        return user;
    }

    /**
     * UM03 - Verify Account
     */
    public VerifyResult verifyAccount(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account with that email"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            return new VerifyResult(user, true);
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new BadRequestException("That verification code is not correct");
        }

        if (user.getVerificationExpiresAt() == null || user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("That verification code has expired, request a new one");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setVerifiedAt(LocalDateTime.now());
        user.setVerificationCode(null);
        user.setVerificationExpiresAt(null);
        userRepository.save(user);

        return new VerifyResult(user, false);
    }

    public boolean resendVerification(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account with that email"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("That account is already verified");
        }

        user.setVerificationCode(generateSixDigitCode());
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        notificationService.dispatch(user.getId(), NotificationTemplate.ACCOUNT_VERIFICATION, Map.of(
                "fullName", user.getFullName(),
                "code", user.getVerificationCode(),
                "expiresInMinutes", 15
        ));

        return true;
    }

    public User verifyMobile(String email, String code) {
        return verifyAccount(email, code).user();
    }

    public boolean resendVerificationCode(String email) {
        return resendVerification(email);
    }

    @Transactional(readOnly = true)
    public String getPendingVerificationCode(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getStatus() == UserStatus.PENDING)
                .map(User::getVerificationCode)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /**
     * UM02 - Login / Authenticate
     */
    public User login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Email or password is wrong"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is wrong");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("This account has been suspended");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * UM04 - View and Update Profile
     */
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    public User updateProfile(Long userId, UserDtos.UpdateProfileRequest req) {
        User user = getProfile(userId);
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(com.starvoicelanka.common.validation.InputValidator.requireText(req.getFullName(), "Full name", 3, 120));
        }
        if (req.getMobile() != null && !req.getMobile().isBlank()) {
            user.setMobile(com.starvoicelanka.common.validation.InputValidator.requireMobile(req.getMobile(), "Mobile number"));
        }
        if (req.getNic() != null) {
            user.setNic(com.starvoicelanka.common.validation.InputValidator.optionalNic(req.getNic()));
        }
        return userRepository.save(user);
    }

    public User updateProfile(Long userId, String fullName, String mobile, String nic) {
        UserDtos.UpdateProfileRequest req = new UserDtos.UpdateProfileRequest();
        req.setFullName(fullName);
        req.setMobile(mobile);
        req.setNic(nic);
        return updateProfile(userId, req);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getProfile(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Your current password is wrong");
        }
        passwordPolicyFactory.createPolicy(user.getRole()).validate(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BadRequestException("Your new password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * UM05 - Reset Password
     */
    public boolean requestPasswordReset(String email) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Intentionally silent to prevent enumeration
            return true;
        }

        User user = userOpt.get();
        user.setResetCode(generateSixDigitCode());
        user.setResetExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        notificationService.dispatch(user.getId(), NotificationTemplate.PASSWORD_RESET, Map.of(
                "fullName", user.getFullName(),
                "code", user.getResetCode(),
                "expiresInMinutes", 15
        ));

        return true;
    }

    public boolean confirmPasswordReset(String email, String code, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("That reset code is not correct"));

        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            throw new BadRequestException("That reset code is not correct");
        }

        if (user.getResetExpiresAt() == null || user.getResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("That reset code has expired");
        }
        passwordPolicyFactory.createPolicy(user.getRole()).validate(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    /**
     * UM06 - Manage Accounts (admin)
     */
    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable, Role role, UserStatus status, String search) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("mobile")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(Role role, UserStatus status, Pageable pageable) {
        return listUsers(pageable, role, status, null);
    }

    public User setStatus(Long userId, UserStatus status, User actor, String reason, String ipAddress) {
        User target = getProfile(userId);
        if (target.getRole() == Role.ADMIN && status == UserStatus.SUSPENDED) {
            throw new ForbiddenException("An admin account cannot be suspended from here");
        }
        if (target.getClosedAt() != null) {
            throw new BadRequestException("That account has been closed");
        }

        String before = target.getStatus().name();
        target.setStatus(status);
        userRepository.save(target);

        writeAudit(AuditAction.STATUS_CHANGED, actor, target, before, status.name(), reason, ipAddress);
        return target;
    }

    /**
     * UM06 - Change a user's role
     */
    public User changeRole(Long userId, Role newRole, User actor, String reason, String ipAddress) {
        User target = getProfile(userId);
        if (target.getClosedAt() != null) {
            throw new BadRequestException("That account has been closed");
        }

        if (actor != null && actor.getId().equals(target.getId())) {
            throw new ForbiddenException("You cannot change your own role - ask another administrator");
        }

        if (target.getRole() == newRole) {
            throw new BadRequestException("That account is already " + newRole);
        }

        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long remainingAdmins = userRepository.countByRoleAndStatusAndClosedAtIsNullAndIdNot(
                    Role.ADMIN, UserStatus.ACTIVE, target.getId());
            if (remainingAdmins == 0) {
                throw new BadRequestException("That is the last active administrator - promote someone else first");
            }
        }

        String before = target.getRole().name();
        target.setRole(newRole);
        userRepository.save(target);

        writeAudit(AuditAction.ROLE_CHANGED, actor, target, before, newRole.name(), reason, ipAddress);
        return target;
    }

    public User changeRole(Long userId, Role newRole, User actor) {
        return changeRole(userId, newRole, actor, "Role change", "127.0.0.1");
    }

    /**
     * UM06 - Close an account (anonymise, never delete)
     */
    public User closeAccount(Long userId, User actor, String reason, String ipAddress, String confirmPassword) {
        User target = getProfile(userId);
        if (target.getClosedAt() != null) {
            throw new BadRequestException("That account is already closed");
        }

        if (confirmPassword != null) {
            if (!passwordEncoder.matches(confirmPassword, target.getPasswordHash())) {
                throw new BadRequestException("That password is wrong");
            }
        }

        if (target.getRole() == Role.ADMIN) {
            long remainingAdmins = userRepository.countByRoleAndStatusAndClosedAtIsNullAndIdNot(
                    Role.ADMIN, UserStatus.ACTIVE, target.getId());
            if (remainingAdmins == 0) {
                throw new BadRequestException("That is the last active administrator - promote someone else first");
            }
        }

        String originalEmail = target.getEmail();
        String suffix = String.format("%08d", target.getId());

        target.setFullName("Closed account");
        target.setEmail("closed-" + suffix + "@starvoice.invalid");
        target.setMobile("0000000000");
        target.setNic(null);
        target.setVerificationCode(null);
        target.setVerificationExpiresAt(null);
        target.setResetCode(null);
        target.setResetExpiresAt(null);
        target.setStatus(UserStatus.SUSPENDED);
        target.setClosedAt(LocalDateTime.now());
        target.setAnonymisedAt(LocalDateTime.now());
        target.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(target);

        writeAudit(AuditAction.ACCOUNT_CLOSED, actor, target, originalEmail, target.getEmail(),
                reason != null ? reason : "Account closed at the request of the account holder", ipAddress);

        return target;
    }

    public User closeAccount(Long userId, User actor, String reason, String ipAddress) {
        return closeAccount(userId, actor, reason, ipAddress, null);
    }

    public AuditLog writeAudit(AuditAction action, User actor, User target, String before, String after,
                               String reason, String ipAddress) {
        // keep free text inside the column sizes so a long reason can never break the save
        if (reason != null && reason.length() > 500) reason = reason.substring(0, 500);
        if (ipAddress != null && ipAddress.length() > 60) ipAddress = ipAddress.substring(0, 60);
        AuditLog logEntry = new AuditLog(
                action,
                actor,
                actor != null ? actor.getEmail() : "system",
                target,
                target != null ? target.getEmail() : null,
                before,
                after,
                reason,
                ipAddress
        );
        return auditLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLog(Long targetUserId, AuditAction action, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (targetUserId != null) {
                predicates.add(cb.equal(root.get("targetUser").get("id"), targetUserId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable);
    }

    /**
     * Admin direct account creation (CREATE in CRUD)
     */
    public User createUser(String fullName, String email, String mobile, String password, Role role, UserStatus status, User actor, String ipAddress) {
        fullName = com.starvoicelanka.common.validation.InputValidator.requireText(fullName, "Full name", 3, 120);
        email = com.starvoicelanka.common.validation.InputValidator.requireEmail(email, "Email");
        if (mobile != null && !mobile.isBlank()) {
            mobile = com.starvoicelanka.common.validation.InputValidator.requireMobile(mobile, "Mobile number");
        }
        Role newRole = role != null ? role : Role.VOTER;
        if (password != null && !password.isBlank()) {
            passwordPolicyFactory.createPolicy(newRole).validate(password);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with that email already exists");
        }
        User user = new User(
                fullName,
                email,
                mobile != null && !mobile.isBlank() ? mobile : "0770000000",
                null,
                passwordEncoder.encode(password != null && !password.isBlank() ? password : "Password123!"),
                newRole,
                status != null ? status : UserStatus.ACTIVE
        );
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setVerifiedAt(LocalDateTime.now());
        }
        User saved = userRepository.save(user);
        if (creditAccountRepository != null) {
            creditAccountRepository.save(new com.starvoicelanka.payment.entity.CreditAccount(saved));
        }
        if (notificationPreferenceRepository != null) {
            notificationPreferenceRepository.save(new com.starvoicelanka.notification.entity.NotificationPreference(saved));
        }
        writeAudit(AuditAction.STATUS_CHANGED, actor, saved, "NONE", saved.getStatus().name(), "Admin created user account", ipAddress);
        return saved;
    }

    /**
     * Delete a user permanently (Admin action - DELETE in CRUD)
     */
    public void deleteUser(Long userId, User actor, String ipAddress) {
        User target = getProfile(userId);

        if (actor != null && actor.getId().equals(target.getId())) {
            throw new ForbiddenException("You cannot delete your own account");
        }

        if (target.getRole() == Role.ADMIN) {
            long remainingAdmins = userRepository.countByRoleAndStatusAndClosedAtIsNullAndIdNot(
                    Role.ADMIN, UserStatus.ACTIVE, target.getId());
            if (remainingAdmins == 0) {
                throw new BadRequestException("Cannot delete the last active administrator - promote someone else first");
            }
        }

        String targetEmail = target.getEmail();
        String targetName = target.getFullName();

        // 1. Clean up or unlink references from dependent entities
        if (creditAccountRepository != null) {
            creditAccountRepository.deleteByUserId(userId);
        }
        if (notificationPreferenceRepository != null) {
            notificationPreferenceRepository.deleteByUserId(userId);
        }
        if (notificationRepository != null) {
            notificationRepository.deleteByUserId(userId);
        }
        if (voteQuotaRepository != null) {
            voteQuotaRepository.deleteByVoterId(userId);
        }
        if (voteRepository != null) {
            voteRepository.deleteByVoterId(userId);
        }
        if (paymentRepository != null) {
            paymentRepository.deleteByUserId(userId);
        }
        if (sponsorRepository != null) {
            sponsorRepository.nullifyAccountManager(userId);
        }
        if (auditLogRepository != null) {
            // The live audit_logs table has target_user_id as NOT NULL (this can
            // differ from what the entity mapping implies, depending on how the
            // database file was first created - ddl-auto:update never loosens an
            // existing NOT NULL). So we DELETE this user's audit rows rather than
            // nullify them - a delete statement works no matter which nullability
            // the column actually has, which a nullify (UPDATE ... = null) does not.
            auditLogRepository.deleteByTargetUserId(userId);
            auditLogRepository.deleteByActorId(userId);
        }

        // 2. Delete the user
        userRepository.delete(target);

        // 3. We intentionally do NOT write a "USER_DELETED" audit row afterwards:
        // target_user_id is NOT NULL, and the user we'd be pointing at no longer
        // exists, so there is no valid value we could put there. The IP address
        // and admin are still captured in the application log instead.
        log.info("User {} <{}> deleted by admin {} from {}", targetName, targetEmail,
                actor != null ? actor.getEmail() : "system", ipAddress);
    }
}
