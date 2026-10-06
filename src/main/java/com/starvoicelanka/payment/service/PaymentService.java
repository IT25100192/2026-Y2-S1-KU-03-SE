package com.starvoicelanka.payment.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ForbiddenException;
import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.payment.dto.PaymentDtos;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentMethod;
import com.starvoicelanka.payment.entity.PaymentStatus;
import com.starvoicelanka.payment.entity.VoteBundle;
import com.starvoicelanka.payment.repository.CreditAccountRepository;
import com.starvoicelanka.payment.repository.PaymentRepository;
import com.starvoicelanka.payment.repository.VoteBundleRepository;
import com.starvoicelanka.payment.service.gateway.GatewayResult;
import com.starvoicelanka.payment.service.gateway.PaymentGateway;
import com.starvoicelanka.payment.service.gateway.PaymentGatewayFactory;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final VoteBundleRepository bundleRepository;
    private final PaymentRepository paymentRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ReceiptPdfService receiptPdfService;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final PaymentGatewayFactory gatewayFactory = new PaymentGatewayFactory();

    public PaymentService(VoteBundleRepository bundleRepository,
                          PaymentRepository paymentRepository,
                          CreditAccountRepository creditAccountRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          ReceiptPdfService receiptPdfService,
                          AppProperties properties) {
        this.bundleRepository = bundleRepository;
        this.paymentRepository = paymentRepository;
        this.creditAccountRepository = creditAccountRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.receiptPdfService = receiptPdfService;
        this.properties = properties;
    }

    /* ---- Bundles --------------------------------------------------------- */

    @Transactional(readOnly = true)
    public List<VoteBundle> listBundles() {
        return bundleRepository.findByIsActiveTrueOrderByPriceLKRAsc();
    }

    @Transactional(readOnly = true)
    public List<VoteBundle> listAllBundles() {
        return bundleRepository.findAllByOrderByPriceLKRAsc();
    }

    public VoteBundle createBundle(PaymentDtos.CreateBundleRequest req) {
        String code = InputValidator.requireCode(req.getCode(), "Bundle code", 60);
        String name = InputValidator.requireText(req.getName(), "Bundle name", 2, 120);
        int credits = InputValidator.requireRange(req.getVoteCredits(), "Vote credits", 1, 100000);
        double price = InputValidator.requireMoney(req.getPriceLKR(), "Price", false, 10_000_000d);
        if (bundleRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ConflictException("A bundle with the code " + code + " already exists");
        }
        VoteBundle bundle = new VoteBundle(
                code,
                name,
                credits,
                price,
                req.getIsActive() != null ? req.getIsActive() : true
        );
        return bundleRepository.save(bundle);
    }

    @Transactional(readOnly = true)
    public VoteBundle getBundle(Long bundleId) {
        return bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bundle not found: " + bundleId));
    }

    /** UPDATE: change a bundle's name, credits, price and active flag. */
    public VoteBundle updateBundle(Long bundleId, String name, Integer voteCredits, Double priceLKR, Boolean active) {
        VoteBundle b = getBundle(bundleId);
        if (name != null && !name.isBlank()) b.setName(InputValidator.requireText(name, "Bundle name", 2, 120));
        if (voteCredits != null) {
            b.setVoteCredits(InputValidator.requireRange(voteCredits, "Vote credits", 1, 100000));
        }
        if (priceLKR != null) {
            b.setPriceLKR(InputValidator.requireMoney(priceLKR, "Price", false, 10_000_000d));
        }
        if (active != null) b.setActive(active);
        return bundleRepository.save(b);
    }

    /** DELETE: remove a bundle that has never been bought (otherwise deactivate it to keep receipts intact). */
    public void deleteBundle(Long bundleId) {
        VoteBundle b = getBundle(bundleId);
        if (paymentRepository.existsByBundleId(bundleId)) {
            throw new BadRequestException("This bundle has been purchased before, so it cannot be deleted. Deactivate it instead.");
        }
        bundleRepository.delete(b);
    }

    /* ---- Credit Account -------------------------------------------------- */

    public CreditAccount getAccount(Long userId) {
        return creditAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    CreditAccount account = new CreditAccount(user);
                    return creditAccountRepository.save(account);
                });
    }

    public CreditAccount consumeCredits(Long userId, int count) {
        CreditAccount account = getAccount(userId);
        if (account.getBalance() < count) {
            throw new BadRequestException(String.format(
                    "Not enough vote credits. You have %d, you need %d. Buy a bundle to keep voting.",
                    account.getBalance(), count));
        }
        account.setBalance(account.getBalance() - count);
        account.setLifetimeSpent(account.getLifetimeSpent() + count);
        return creditAccountRepository.save(account);
    }

    public CreditAccount refundCredits(Long userId, int count) {
        CreditAccount account = getAccount(userId);
        account.setBalance(account.getBalance() + count);
        account.setLifetimeSpent(Math.max(0, account.getLifetimeSpent() - count));
        return creditAccountRepository.save(account);
    }

    /* ---- Purchase & Gateway ---------------------------------------------- */

    private String nextReceiptNo() {
        int year = LocalDateTime.now().getYear();
        int serial = 100000 + random.nextInt(900000);
        return String.format("SVL-%d-%d", year, serial);
    }

    /**
     * STRATEGY + FACTORY: the factory gives back the strategy for the chosen method,
     * and the strategy takes the money. No if-else on the payment method here.
     */
    private GatewayResult callGateway(double amountLKR, PaymentMethod method, String card) {
        PaymentGateway gateway = gatewayFactory.createGateway(method);
        return gateway.pay(amountLKR, card);
    }

    public PaymentDtos.PurchaseResultDto purchaseBundle(Long userId, String bundleCode, PaymentMethod method, String card) {
        InputValidator.requireText(bundleCode, "Bundle", 1, 60);
        if (method == null || method == PaymentMethod.CARD) {
            card = InputValidator.requireCardNumber(card);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        VoteBundle bundle = bundleRepository.findByCodeIgnoreCaseAndIsActiveTrue(bundleCode)
                .orElseThrow(() -> new ResourceNotFoundException("That vote bundle is not available"));

        Payment payment = new Payment(
                user,
                bundle,
                nextReceiptNo(),
                "PENDING",
                bundle.getPriceLKR(),
                bundle.getVoteCredits(),
                method != null ? method : PaymentMethod.CARD,
                PaymentStatus.PENDING
        );
        payment = paymentRepository.save(payment);

        GatewayResult result = callGateway(bundle.getPriceLKR(), payment.getMethod(), card);

        if (!result.approved()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setReference(result.reference());
            payment.setFailureReason(result.reason());
            paymentRepository.save(payment);
            throw new BadRequestException("Payment failed: " + result.reason());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setReference(result.reference());
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        CreditAccount account = getAccount(userId);
        account.setBalance(account.getBalance() + bundle.getVoteCredits());
        account.setLifetimePurchased(account.getLifetimePurchased() + bundle.getVoteCredits());
        creditAccountRepository.save(account);

        notificationService.dispatch(userId, NotificationTemplate.PAYMENT_RECEIPT, Map.of(
                "receiptNo", payment.getReceiptNo(),
                "amount", payment.getAmountLKR(),
                "voteCredits", payment.getVoteCredits(),
                "reference", payment.getReference()
        ));

        return new PaymentDtos.PurchaseResultDto(PaymentDtos.PaymentDto.from(payment), account.getBalance());
    }

    /* ---- History & Refund ------------------------------------------------ */

    @Transactional(readOnly = true)
    public Page<Payment> listPayments(Long userId, PaymentStatus status, Pageable pageable) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return paymentRepository.findAll(spec, pageable);
    }

    public Payment refundPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        reason = InputValidator.optionalText(reason, "Refund reason", 500);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only a successful payment can be refunded, this one is " + payment.getStatus());
        }

        CreditAccount account = getAccount(payment.getUser().getId());
        if (account.getBalance() < payment.getVoteCredits()) {
            throw new BadRequestException("Those credits have already been spent on votes, so the payment cannot be refunded");
        }

        account.setBalance(account.getBalance() - payment.getVoteCredits());
        account.setLifetimePurchased(account.getLifetimePurchased() - payment.getVoteCredits());
        creditAccountRepository.save(account);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);
        payment = paymentRepository.save(payment);

        notificationService.dispatch(payment.getUser().getId(), NotificationTemplate.REFUND_ISSUED, Map.of(
                "receiptNo", payment.getReceiptNo(),
                "amount", payment.getAmountLKR(),
                "method", payment.getMethod().name(),
                "reason", reason != null ? reason : "Requested by account holder"
        ));

        return payment;
    }

    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);
    }

    /* ---- Receipt & PDF --------------------------------------------------- */

    @Transactional(readOnly = true)
    public PaymentDtos.ReceiptDto getReceipt(Long paymentId, User requestingUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        boolean isOwner = requestingUser != null && requestingUser.getId().equals(payment.getUser().getId());
        boolean isAdmin = requestingUser != null && requestingUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("That receipt belongs to another account");
        }

        PaymentDtos.ReceiptBilledTo billedTo = new PaymentDtos.ReceiptBilledTo(
                payment.getUser().getFullName(),
                payment.getUser().getEmail(),
                payment.getUser().getMobile()
        );

        PaymentDtos.ReceiptLineItem lineItem = new PaymentDtos.ReceiptLineItem(
                payment.getBundle().getName() + " (" + payment.getVoteCredits() + " vote credits)",
                payment.getAmountLKR()
        );

        return new PaymentDtos.ReceiptDto(
                payment.getReceiptNo(),
                payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(),
                billedTo,
                lineItem,
                payment.getMethod(),
                payment.getReference(),
                payment.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public byte[] renderReceiptPdf(Long paymentId, User requestingUser) {
        PaymentDtos.ReceiptDto receipt = getReceipt(paymentId, requestingUser);
        return receiptPdfService.generateReceiptPdf(receipt);
    }

    /* ---- Webhook HMAC ---------------------------------------------------- */

    public String signWebhook(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(properties.getPaymentWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate webhook HMAC", e);
        }
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (signature == null || rawBody == null) return false;
        String expected = signWebhook(rawBody);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    public PaymentDtos.WebhookResultDto handleGatewayWebhook(PaymentDtos.WebhookPayload payload) {
        if (payload.getEventId() == null || payload.getReference() == null || payload.getStatus() == null) {
            throw new BadRequestException("A webhook needs eventId, reference and status");
        }

        // Idempotency: Replay of an event already applied
        Optional<Payment> seen = paymentRepository.findByWebhookEventId(payload.getEventId());
        if (seen.isPresent()) {
            Payment p = seen.get();
            return new PaymentDtos.WebhookResultDto(false, "Already processed", p.getReceiptNo(), p.getStatus(), p.getStatus());
        }

        Payment payment = paymentRepository.findByReference(payload.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("No payment with reference " + payload.getReference()));

        if (payload.getAmountLKR() != null && Math.abs(payload.getAmountLKR() - payment.getAmountLKR()) > 0.01) {
            payment.setReconciliationNote("Webhook amount LKR " + payload.getAmountLKR() + " does not match LKR " + payment.getAmountLKR());
            paymentRepository.save(payment);
            throw new BadRequestException(payment.getReconciliationNote());
        }

        PaymentStatus previous = payment.getStatus();
        payment.setWebhookEventId(payload.getEventId());
        payment.setGatewayConfirmedAt(LocalDateTime.now());

        if (payload.getStatus() == PaymentStatus.SUCCESS && previous != PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            if (payment.getPaidAt() == null) payment.setPaidAt(LocalDateTime.now());
            CreditAccount account = getAccount(payment.getUser().getId());
            account.setBalance(account.getBalance() + payment.getVoteCredits());
            account.setLifetimePurchased(account.getLifetimePurchased() + payment.getVoteCredits());
            creditAccountRepository.save(account);
        } else if (payload.getStatus() == PaymentStatus.FAILED && previous == PaymentStatus.SUCCESS) {
            CreditAccount account = getAccount(payment.getUser().getId());
            int clawBack = Math.min(account.getBalance(), payment.getVoteCredits());
            account.setBalance(account.getBalance() - clawBack);
            creditAccountRepository.save(account);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Reversed by the gateway after approval");
            if (clawBack < payment.getVoteCredits()) {
                payment.setReconciliationNote(String.format("Reversed after %d credit(s) were already spent", payment.getVoteCredits() - clawBack));
            }
        } else if (payload.getStatus() == PaymentStatus.REFUNDED && previous != PaymentStatus.REFUNDED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            if (payment.getRefundReason() == null) payment.setRefundReason("Refunded at the gateway");
        }

        paymentRepository.save(payment);
        return new PaymentDtos.WebhookResultDto(true, "Applied", payment.getReceiptNo(), previous, payment.getStatus());
    }

    /* ---- Daily Reconciliation -------------------------------------------- */

    public List<PaymentDtos.SettlementRow> parseSettlementCsv(String csvText) {
        if (csvText == null || csvText.isBlank()) return List.of();
        String[] lines = csvText.trim().split("\\r?\\n");
        if (lines.length == 0) return List.of();

        String[] header = lines[0].split(",");
        int refIdx = -1, amountIdx = -1, statusIdx = -1;
        for (int i = 0; i < header.length; i++) {
            String col = header[i].trim().toLowerCase();
            if ("reference".equals(col)) refIdx = i;
            if ("amount".equals(col)) amountIdx = i;
            if ("status".equals(col)) statusIdx = i;
        }

        if (refIdx == -1 || amountIdx == -1) {
            throw new BadRequestException("The settlement file needs at least a reference and an amount column");
        }

        List<PaymentDtos.SettlementRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cells = line.split(",");
            String ref = cells[refIdx].trim();
            double amt = Double.parseDouble(cells[amountIdx].trim());
            String st = statusIdx != -1 && statusIdx < cells.length ? cells[statusIdx].trim().toUpperCase() : "SUCCESS";
            rows.add(new PaymentDtos.SettlementRow(ref, amt, st));
        }
        return rows;
    }

    public PaymentDtos.ReconciliationReportDto dailyReconciliation(LocalDate date, List<PaymentDtos.SettlementRow> settlementRows) {
        LocalDate day = date != null ? date : LocalDate.now();
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.atTime(23, 59, 59, 999999999);

        List<Payment> payments = paymentRepository.findByCreatedAtBetweenAndStatusNot(start, end, PaymentStatus.PENDING);

        Map<String, PaymentDtos.SettlementRow> gatewayByRef = new HashMap<>();
        for (PaymentDtos.SettlementRow row : settlementRows) {
            gatewayByRef.put(row.getReference(), row);
        }

        Map<String, Payment> ledgerByRef = new HashMap<>();
        for (Payment p : payments) {
            ledgerByRef.put(p.getReference(), p);
        }

        List<String> matched = new ArrayList<>();
        List<Object> missingInGateway = new ArrayList<>();
        List<Object> amountMismatch = new ArrayList<>();
        List<Object> statusMismatch = new ArrayList<>();

        for (Payment p : payments) {
            PaymentDtos.SettlementRow row = gatewayByRef.get(p.getReference());
            if (row == null) {
                if (p.getStatus() != PaymentStatus.FAILED) {
                    missingInGateway.add(Map.of(
                            "receiptNo", p.getReceiptNo(),
                            "reference", p.getReference(),
                            "amountLKR", p.getAmountLKR(),
                            "status", p.getStatus().name(),
                            "user", p.getUser().getEmail()
                    ));
                }
                continue;
            }

            if (Math.abs(row.getAmountLKR() - p.getAmountLKR()) > 0.01) {
                amountMismatch.add(Map.of(
                        "receiptNo", p.getReceiptNo(),
                        "reference", p.getReference(),
                        "ledgerAmountLKR", p.getAmountLKR(),
                        "gatewayAmountLKR", row.getAmountLKR(),
                        "differenceLKR", Math.round((row.getAmountLKR() - p.getAmountLKR()) * 100.0) / 100.0
                ));
            } else if (!row.getStatus().equalsIgnoreCase(p.getStatus().name())) {
                statusMismatch.add(Map.of(
                        "receiptNo", p.getReceiptNo(),
                        "reference", p.getReference(),
                        "ledgerStatus", p.getStatus().name(),
                        "gatewayStatus", row.getStatus()
                ));
            } else {
                matched.add(p.getReceiptNo());
                p.setReconciledAt(LocalDateTime.now());
                p.setReconciliationNote(null);
                paymentRepository.save(p);
            }
        }

        List<Object> missingInLedger = new ArrayList<>();
        for (PaymentDtos.SettlementRow row : settlementRows) {
            if (!ledgerByRef.containsKey(row.getReference())) {
                missingInLedger.add(Map.of(
                        "reference", row.getReference(),
                        "amountLKR", row.getAmountLKR(),
                        "status", row.getStatus()
                ));
            }
        }

        double settledTotal = settlementRows.stream().mapToDouble(PaymentDtos.SettlementRow::getAmountLKR).sum();
        double ledgerTotal = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .mapToDouble(Payment::getAmountLKR).sum();

        boolean clean = missingInGateway.isEmpty() && missingInLedger.isEmpty() && amountMismatch.isEmpty() && statusMismatch.isEmpty();

        Map<String, Object> totals = Map.of(
                "ledgerLKR", Math.round(ledgerTotal * 100.0) / 100.0,
                "gatewayLKR", Math.round(settledTotal * 100.0) / 100.0,
                "differenceLKR", Math.round((settledTotal - ledgerTotal) * 100.0) / 100.0
        );

        return new PaymentDtos.ReconciliationReportDto(
                day.toString(),
                payments.size(),
                settlementRows.size(),
                matched.size(),
                missingInGateway,
                missingInLedger,
                amountMismatch,
                statusMismatch,
                totals,
                clean
        );
    }
}
