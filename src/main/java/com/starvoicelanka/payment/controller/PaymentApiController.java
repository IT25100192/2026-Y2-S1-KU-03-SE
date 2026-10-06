package com.starvoicelanka.payment.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.PagedResponse;
import com.starvoicelanka.common.exception.UnauthorizedException;
import com.starvoicelanka.payment.dto.PaymentDtos;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentStatus;
import com.starvoicelanka.payment.entity.VoteBundle;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentApiController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("You need to be logged in");
        }
        return userService.getUserByEmail(userDetails.getUsername());
    }

    // PM01 - public price list
    @GetMapping("/bundles")
    public ApiResponse<List<VoteBundle>> listBundles() {
        return ApiResponse.success(paymentService.listBundles());
    }

    // PM01 - admin creates bundle
    @PostMapping("/bundles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoteBundle>> createBundle(@Valid @RequestBody PaymentDtos.CreateBundleRequest request) {
        VoteBundle bundle = paymentService.createBundle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(bundle));
    }

    // Balance of current user
    @GetMapping("/balance")
    public ApiResponse<CreditAccount> balance(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ApiResponse.success(paymentService.getAccount(user.getId()));
    }

    // PM02 - purchase bundle
    @PostMapping("/purchase")
    public ApiResponse<PaymentDtos.PurchaseResultDto> purchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentDtos.PurchaseRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (!user.isMobileVerified()) {
            throw new UnauthorizedException("Please verify your mobile number before purchasing vote credits");
        }
        PaymentDtos.PurchaseResultDto result = paymentService.purchaseBundle(
                user.getId(),
                request.getBundleCode(),
                request.getMethod(),
                request.getCardNumber()
        );
        return ApiResponse.success(result);
    }

    // PM06 - gateway webhook
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentDtos.WebhookResultDto>> gatewayWebhook(
            @RequestHeader(value = "x-starvoice-signature", required = false) String signature,
            @RequestBody String rawBody,
            @Valid @RequestBody PaymentDtos.WebhookPayload payload) {
        if (!paymentService.verifyWebhookSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bad webhook signature"));
        }
        PaymentDtos.WebhookResultDto result = paymentService.handleGatewayWebhook(payload);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // PM03 - my payments
    @GetMapping("/me")
    public ApiResponse<List<PaymentDtos.PaymentDto>> myPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        User user = getAuthenticatedUser(userDetails);
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> result = paymentService.listPayments(user.getId(), status, pageable);
        List<PaymentDtos.PaymentDto> dtos = result.getContent().stream().map(PaymentDtos.PaymentDto::from).toList();

        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(dtos, meta);
    }

    // PM03 - all payments (admin view)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PaymentDtos.PaymentDto>> allPayments(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> result = paymentService.listPayments(userId, status, pageable);
        List<PaymentDtos.PaymentDto> dtos = result.getContent().stream().map(PaymentDtos.PaymentDto::from).toList();

        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(dtos, meta);
    }

    // PM06 - daily reconciliation
    @PostMapping("/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentDtos.ReconciliationReportDto> reconcile(
            @RequestBody(required = false) PaymentDtos.ReconcileRequest request) {
        java.time.LocalDate localDate = null;
        if (request != null && request.getDate() != null && !request.getDate().isBlank()) {
            try {
                localDate = java.time.LocalDate.parse(request.getDate());
            } catch (Exception ignored) {}
        }
        List<PaymentDtos.SettlementRow> rows = request != null && request.getRows() != null ? request.getRows() : List.of();
        PaymentDtos.ReconciliationReportDto report = paymentService.dailyReconciliation(localDate, rows);
        return ApiResponse.success(report);
    }

    // PM05 - JSON receipt
    @GetMapping("/{id}/receipt")
    public ApiResponse<PaymentDtos.ReceiptDto> receipt(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        PaymentDtos.ReceiptDto receiptDto = paymentService.getReceipt(id, user);
        return ApiResponse.success(receiptDto);
    }

    // PM05 - PDF receipt
    @GetMapping("/{id}/receipt.pdf")
    public ResponseEntity<byte[]> receiptPdf(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        PaymentDtos.ReceiptDto receiptDto = paymentService.getReceipt(id, user);
        byte[] pdfBytes = paymentService.renderReceiptPdf(id, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + receiptDto.receiptNo() + ".pdf\"")
                .body(pdfBytes);
    }

    // PM04 - refund
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentDtos.PaymentDto> refund(
            @PathVariable("id") Long id,
            @Valid @RequestBody PaymentDtos.RefundRequest request) {
        Payment payment = paymentService.refundPayment(id, request.getReason());
        return ApiResponse.success(PaymentDtos.PaymentDto.from(payment));
    }
}
