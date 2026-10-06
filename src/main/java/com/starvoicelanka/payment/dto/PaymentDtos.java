package com.starvoicelanka.payment.dto;

import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentMethod;
import com.starvoicelanka.payment.entity.PaymentStatus;
import com.starvoicelanka.payment.entity.VoteBundle;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDtos {

    public static class CreateBundleRequest {
        @NotBlank(message = "Bundle code is required")
        @Size(min = 3, message = "Code must be at least 3 characters")
        private String code;

        @NotBlank(message = "Bundle name is required")
        private String name;

        @NotNull(message = "Vote credits is required")
        @Min(value = 1, message = "Vote credits must be at least 1")
        private Integer voteCredits;

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price cannot be negative")
        private Double priceLKR;

        private Boolean isActive = true;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getVoteCredits() { return voteCredits; }
        public void setVoteCredits(Integer voteCredits) { this.voteCredits = voteCredits; }
        public Double getPriceLKR() { return priceLKR; }
        public void setPriceLKR(Double priceLKR) { this.priceLKR = priceLKR; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    public static class PurchaseRequest {
        @NotBlank(message = "Bundle code is required")
        private String bundleCode;

        private PaymentMethod method = PaymentMethod.CARD;
        private String card;

        public String getBundleCode() { return bundleCode; }
        public void setBundleCode(String bundleCode) { this.bundleCode = bundleCode; }
        public PaymentMethod getMethod() { return method; }
        public void setMethod(PaymentMethod method) { this.method = method; }
        public String getCard() { return card; }
        public void setCard(String card) { this.card = card; }
        public String getCardNumber() { return card; }
        public void setCardNumber(String cardNumber) { this.card = cardNumber; }
    }

    public static class RefundRequest {
        @NotBlank(message = "Refund reason is required")
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class PurchaseResultDto {
        private PaymentDto payment;
        private int balance;

        public PurchaseResultDto(PaymentDto payment, int balance) {
            this.payment = payment;
            this.balance = balance;
        }

        public PaymentDto getPayment() { return payment; }
        public int getBalance() { return balance; }
        public PaymentDto payment() { return payment; }
        public int balance() { return balance; }
    }

    public static class AccountBalanceDto {
        private int balance;
        private int lifetimePurchased;
        private int lifetimeSpent;

        public AccountBalanceDto(int balance, int lifetimePurchased, int lifetimeSpent) {
            this.balance = balance;
            this.lifetimePurchased = lifetimePurchased;
            this.lifetimeSpent = lifetimeSpent;
        }

        public int getBalance() { return balance; }
        public int getLifetimePurchased() { return lifetimePurchased; }
        public int getLifetimeSpent() { return lifetimeSpent; }
    }

    public static class PaymentDto {
        private Long id;
        private Long userId;
        private String userEmail;
        private String userName;
        private Long bundleId;
        private String bundleName;
        private String receiptNo;
        private String reference;
        private double amountLKR;
        private int voteCredits;
        private PaymentMethod method;
        private PaymentStatus status;
        private LocalDateTime paidAt;
        private LocalDateTime refundedAt;
        private String refundReason;
        private String failureReason;
        private LocalDateTime createdAt;

        public static PaymentDto from(Payment p) {
            if (p == null) return null;
            PaymentDto dto = new PaymentDto();
            dto.id = p.getId();
            dto.userId = p.getUser() != null ? p.getUser().getId() : null;
            dto.userEmail = p.getUser() != null ? p.getUser().getEmail() : null;
            dto.userName = p.getUser() != null ? p.getUser().getFullName() : null;
            dto.bundleId = p.getBundle() != null ? p.getBundle().getId() : null;
            dto.bundleName = p.getBundle() != null ? p.getBundle().getName() : null;
            dto.receiptNo = p.getReceiptNo();
            dto.reference = p.getReference();
            dto.amountLKR = p.getAmountLKR();
            dto.voteCredits = p.getVoteCredits();
            dto.method = p.getMethod();
            dto.status = p.getStatus();
            dto.paidAt = p.getPaidAt();
            dto.refundedAt = p.getRefundedAt();
            dto.refundReason = p.getRefundReason();
            dto.failureReason = p.getFailureReason();
            dto.createdAt = p.getCreatedAt();
            return dto;
        }

        public Long getId() { return id; }
        public Long getUserId() { return userId; }
        public String getUserEmail() { return userEmail; }
        public String getUserName() { return userName; }
        public Long getBundleId() { return bundleId; }
        public String getBundleName() { return bundleName; }
        public String getReceiptNo() { return receiptNo; }
        public String getReference() { return reference; }
        public double getAmountLKR() { return amountLKR; }
        public int getVoteCredits() { return voteCredits; }
        public PaymentMethod getMethod() { return method; }
        public PaymentStatus getStatus() { return status; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public LocalDateTime getRefundedAt() { return refundedAt; }
        public String getRefundReason() { return refundReason; }
        public String getFailureReason() { return failureReason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class ReceiptBilledTo {
        private String name;
        private String email;
        private String mobile;

        public ReceiptBilledTo(String name, String email, String mobile) {
            this.name = name;
            this.email = email;
            this.mobile = mobile;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getMobile() { return mobile; }
    }

    public static class ReceiptLineItem {
        private String description;
        private double amountLKR;

        public ReceiptLineItem(String description, double amountLKR) {
            this.description = description;
            this.amountLKR = amountLKR;
        }

        public String getDescription() { return description; }
        public double getAmountLKR() { return amountLKR; }
    }

    public static class ReceiptDto {
        private String receiptNo;
        private LocalDateTime issuedAt;
        private ReceiptBilledTo billedTo;
        private ReceiptLineItem lineItem;
        private PaymentMethod method;
        private String reference;
        private PaymentStatus status;

        public ReceiptDto(String receiptNo, LocalDateTime issuedAt, ReceiptBilledTo billedTo, ReceiptLineItem lineItem, PaymentMethod method, String reference, PaymentStatus status) {
            this.receiptNo = receiptNo;
            this.issuedAt = issuedAt;
            this.billedTo = billedTo;
            this.lineItem = lineItem;
            this.method = method;
            this.reference = reference;
            this.status = status;
        }

        public String getReceiptNo() { return receiptNo; }
        public String receiptNo() { return receiptNo; }
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public ReceiptBilledTo getBilledTo() { return billedTo; }
        public ReceiptLineItem getLineItem() { return lineItem; }
        public PaymentMethod getMethod() { return method; }
        public String getReference() { return reference; }
        public PaymentStatus getStatus() { return status; }
    }

    public static class WebhookPayload {
        private String eventId;
        private String reference;
        private PaymentStatus status;
        private Double amountLKR;

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public PaymentStatus getStatus() { return status; }
        public void setStatus(PaymentStatus status) { this.status = status; }
        public Double getAmountLKR() { return amountLKR; }
        public void setAmountLKR(Double amountLKR) { this.amountLKR = amountLKR; }
    }

    public static class WebhookResultDto {
        private boolean applied;
        private String reason;
        private String receiptNo;
        private PaymentStatus from;
        private PaymentStatus to;

        public WebhookResultDto(boolean applied, String reason, String receiptNo, PaymentStatus from, PaymentStatus to) {
            this.applied = applied;
            this.reason = reason;
            this.receiptNo = receiptNo;
            this.from = from;
            this.to = to;
        }

        public boolean isApplied() { return applied; }
        public String getReason() { return reason; }
        public String getReceiptNo() { return receiptNo; }
        public PaymentStatus getFrom() { return from; }
        public PaymentStatus getTo() { return to; }
    }

    public static class SettlementRow {
        private String reference;
        private double amountLKR;
        private String status;

        public SettlementRow(String reference, double amountLKR, String status) {
            this.reference = reference;
            this.amountLKR = amountLKR;
            this.status = status;
        }

        public String getReference() { return reference; }
        public double getAmountLKR() { return amountLKR; }
        public String getStatus() { return status; }
    }

    public static class ReconcileRequest {
        private String date;
        private List<SettlementRow> rows;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public List<SettlementRow> getRows() { return rows; }
        public void setRows(List<SettlementRow> rows) { this.rows = rows; }
    }

    public static class ReconciliationReportDto {
        private String date;
        private int ledgerRows;
        private int gatewayRows;
        private int matched;
        private List<Object> missingInGateway;
        private List<Object> missingInLedger;
        private List<Object> amountMismatch;
        private List<Object> statusMismatch;
        private Object totals;
        private boolean clean;

        public ReconciliationReportDto(String date, int ledgerRows, int gatewayRows, int matched,
                                       List<Object> missingInGateway, List<Object> missingInLedger,
                                       List<Object> amountMismatch, List<Object> statusMismatch,
                                       Object totals, boolean clean) {
            this.date = date;
            this.ledgerRows = ledgerRows;
            this.gatewayRows = gatewayRows;
            this.matched = matched;
            this.missingInGateway = missingInGateway;
            this.missingInLedger = missingInLedger;
            this.amountMismatch = amountMismatch;
            this.statusMismatch = statusMismatch;
            this.totals = totals;
            this.clean = clean;
        }

        public String getDate() { return date; }
        public int getLedgerRows() { return ledgerRows; }
        public int getGatewayRows() { return gatewayRows; }
        public int getMatched() { return matched; }
        public List<Object> getMissingInGateway() { return missingInGateway; }
        public List<Object> getMissingInLedger() { return missingInLedger; }
        public List<Object> getAmountMismatch() { return amountMismatch; }
        public List<Object> getStatusMismatch() { return statusMismatch; }
        public Object getTotals() { return totals; }
        public boolean isClean() { return clean; }
    }
}
