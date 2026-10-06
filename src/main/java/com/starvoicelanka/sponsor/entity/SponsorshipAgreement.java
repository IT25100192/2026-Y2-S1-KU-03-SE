package com.starvoicelanka.sponsor.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.contestant.entity.Season;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sponsorship_agreements")
public class SponsorshipAgreement extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String agreementNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private SponsorshipPackage packageRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private double contractValueLKR;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(length = 500)
    private String bannerImageUrl;

    @Column(length = 500)
    private String clickThroughUrl;

    private LocalDateTime terminatedAt;

    @Column(length = 500)
    private String terminationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_from_id")
    private SponsorshipAgreement renewedFrom;

    private LocalDateTime renewedAt;

    private Double upliftPercent;

    public SponsorshipAgreement() {}

    public String getAgreementNo() {
        return agreementNo;
    }

    public void setAgreementNo(String agreementNo) {
        this.agreementNo = agreementNo;
    }

    public Sponsor getSponsor() {
        return sponsor;
    }

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
    }

    public SponsorshipPackage getPackageRef() {
        return packageRef;
    }

    public void setPackageRef(SponsorshipPackage packageRef) {
        this.packageRef = packageRef;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public double getContractValueLKR() {
        return contractValueLKR;
    }

    public void setContractValueLKR(double contractValueLKR) {
        this.contractValueLKR = contractValueLKR;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public void setStatus(AgreementStatus status) {
        this.status = status;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public void setClickThroughUrl(String clickThroughUrl) {
        this.clickThroughUrl = clickThroughUrl;
    }

    public LocalDateTime getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(LocalDateTime terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public SponsorshipAgreement getRenewedFrom() {
        return renewedFrom;
    }

    public void setRenewedFrom(SponsorshipAgreement renewedFrom) {
        this.renewedFrom = renewedFrom;
    }

    public LocalDateTime getRenewedAt() {
        return renewedAt;
    }

    public void setRenewedAt(LocalDateTime renewedAt) {
        this.renewedAt = renewedAt;
    }

    public Double getUpliftPercent() {
        return upliftPercent;
    }

    public void setUpliftPercent(Double upliftPercent) {
        this.upliftPercent = upliftPercent;
    }
}
