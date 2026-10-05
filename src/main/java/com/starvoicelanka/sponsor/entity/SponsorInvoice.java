package com.starvoicelanka.sponsor.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sponsor_invoices", indexes = {
        @Index(name = "idx_invoice_sponsor_created", columnList = "sponsor_id, created_at")
})
public class SponsorInvoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private SponsorshipAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @Column(nullable = false)
    private double amountLKR;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    private LocalDateTime paidAt;

    @Column(length = 30)
    private String paymentMethod = "BANK_TRANSFER";

    @Column(length = 100)
    private String paymentReference;

    @Column(length = 500)
    private String cancelledReason;

    public SponsorInvoice() {}

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public SponsorshipAgreement getAgreement() {
        return agreement;
    }

    public void setAgreement(SponsorshipAgreement agreement) {
        this.agreement = agreement;
    }

    public Sponsor getSponsor() {
        return sponsor;
    }

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
    }

    public double getAmountLKR() {
        return amountLKR;
    }

    public void setAmountLKR(double amountLKR) {
        this.amountLKR = amountLKR;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }
}
