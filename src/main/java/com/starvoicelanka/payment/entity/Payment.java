package com.starvoicelanka.payment.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_receipt", columnList = "receipt_no", unique = true),
        @Index(name = "idx_payment_reference", columnList = "reference"),
        @Index(name = "idx_payment_webhook_event", columnList = "webhook_event_id"),
        @Index(name = "idx_payment_user_created", columnList = "user_id, created_at")
})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private VoteBundle bundle;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 60)
    private String receiptNo;

    @Column(nullable = false, length = 100)
    private String reference;

    @Column(name = "amount_lkr", nullable = false)
    private double amountLKR;

    @Column(name = "vote_credits", nullable = false)
    private int voteCredits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "gateway_confirmed_at")
    private LocalDateTime gatewayConfirmedAt;

    @Column(name = "webhook_event_id", length = 120)
    private String webhookEventId;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciliation_note", length = 500)
    private String reconciliationNote;

    public Payment() {}

    public Payment(User user, VoteBundle bundle, String receiptNo, String reference, double amountLKR, int voteCredits, PaymentMethod method, PaymentStatus status) {
        this.user = user;
        this.bundle = bundle;
        this.receiptNo = receiptNo;
        this.reference = reference;
        this.amountLKR = amountLKR;
        this.voteCredits = voteCredits;
        this.method = method != null ? method : PaymentMethod.CARD;
        this.status = status != null ? status : PaymentStatus.PENDING;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public VoteBundle getBundle() { return bundle; }
    public void setBundle(VoteBundle bundle) { this.bundle = bundle; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public double getAmountLKR() { return amountLKR; }
    public void setAmountLKR(double amountLKR) { this.amountLKR = amountLKR; }

    public int getVoteCredits() { return voteCredits; }
    public void setVoteCredits(int voteCredits) { this.voteCredits = voteCredits; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getGatewayConfirmedAt() { return gatewayConfirmedAt; }
    public void setGatewayConfirmedAt(LocalDateTime gatewayConfirmedAt) { this.gatewayConfirmedAt = gatewayConfirmedAt; }

    public String getWebhookEventId() { return webhookEventId; }
    public void setWebhookEventId(String webhookEventId) { this.webhookEventId = webhookEventId; }

    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }

    public String getReconciliationNote() { return reconciliationNote; }
    public void setReconciliationNote(String reconciliationNote) { this.reconciliationNote = reconciliationNote; }
}
