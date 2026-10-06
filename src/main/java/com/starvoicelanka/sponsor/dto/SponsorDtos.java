package com.starvoicelanka.sponsor.dto;

import com.starvoicelanka.sponsor.entity.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SponsorDtos {

    public static class RegisterSponsorRequest {
        @NotBlank(message = "Company name is required")
        private String companyName;
        private String industry;
        private String website;
        private String logoUrl;
        @NotBlank(message = "Contact name is required")
        private String contactName;
        @NotBlank(message = "Contact email is required")
        @Email(message = "Valid contact email is required")
        private String contactEmail;
        private String contactPhone;
        private Long accountManagerId;

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Long getAccountManagerId() { return accountManagerId; }
        public void setAccountManagerId(Long accountManagerId) { this.accountManagerId = accountManagerId; }
    }

    public static class UpdateSponsorRequest {
        private String companyName;
        private String industry;
        private String website;
        private String logoUrl;
        private String contactName;
        private String contactEmail;
        private String contactPhone;
        private Boolean isActive;
        private Long accountManagerId;

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Long getAccountManagerId() { return accountManagerId; }
        public void setAccountManagerId(Long accountManagerId) { this.accountManagerId = accountManagerId; }
    }

    public static class CreatePackageRequest {
        @NotNull(message = "Tier is required")
        private SponsorshipTier tier;
        @NotBlank(message = "Package name is required")
        private String name;
        @Min(value = 0, message = "Price cannot be negative")
        private double priceLKR;
        private int guaranteedImpressions = 0;
        private int bannerSlotsPerRound = 1;
        private boolean logoOnLeaderboard = false;
        private boolean namingRights = false;

        public SponsorshipTier getTier() { return tier; }
        public void setTier(SponsorshipTier tier) { this.tier = tier; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPriceLKR() { return priceLKR; }
        public void setPriceLKR(double priceLKR) { this.priceLKR = priceLKR; }
        public int getGuaranteedImpressions() { return guaranteedImpressions; }
        public void setGuaranteedImpressions(int guaranteedImpressions) { this.guaranteedImpressions = guaranteedImpressions; }
        public int getBannerSlotsPerRound() { return bannerSlotsPerRound; }
        public void setBannerSlotsPerRound(int bannerSlotsPerRound) { this.bannerSlotsPerRound = bannerSlotsPerRound; }
        public boolean isLogoOnLeaderboard() { return logoOnLeaderboard; }
        public void setLogoOnLeaderboard(boolean logoOnLeaderboard) { this.logoOnLeaderboard = logoOnLeaderboard; }
        public boolean isNamingRights() { return namingRights; }
        public void setNamingRights(boolean namingRights) { this.namingRights = namingRights; }
    }

    public static class CreateAgreementRequest {
        @NotNull(message = "Sponsor ID is required")
        private Long sponsorId;
        @NotNull(message = "Package tier is required")
        private SponsorshipTier packageTier;
        private Long seasonId;
        private Double contractValueLKR;
        @NotNull(message = "Start date is required")
        private LocalDateTime startsAt;
        @NotNull(message = "End date is required")
        private LocalDateTime endsAt;
        private String bannerImageUrl;
        private String clickThroughUrl;

        public Long getSponsorId() { return sponsorId; }
        public void setSponsorId(Long sponsorId) { this.sponsorId = sponsorId; }
        public SponsorshipTier getPackageTier() { return packageTier; }
        public void setPackageTier(SponsorshipTier packageTier) { this.packageTier = packageTier; }
        public Long getSeasonId() { return seasonId; }
        public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
        public Double getContractValueLKR() { return contractValueLKR; }
        public void setContractValueLKR(Double contractValueLKR) { this.contractValueLKR = contractValueLKR; }
        public LocalDateTime getStartsAt() { return startsAt; }
        public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
        public LocalDateTime getEndsAt() { return endsAt; }
        public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
        public String getBannerImageUrl() { return bannerImageUrl; }
        public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
        public String getClickThroughUrl() { return clickThroughUrl; }
        public void setClickThroughUrl(String clickThroughUrl) { this.clickThroughUrl = clickThroughUrl; }
    }

    public static class TerminateAgreementRequest {
        @NotBlank(message = "Reason is required")
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RenewAgreementRequest {
        private Long seasonId;
        private Double upliftPercent = 0.0;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Double contractValueLKR;
        private boolean activate = false;

        public Long getSeasonId() { return seasonId; }
        public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
        public Double getUpliftPercent() { return upliftPercent; }
        public void setUpliftPercent(Double upliftPercent) { this.upliftPercent = upliftPercent; }
        public LocalDateTime getStartsAt() { return startsAt; }
        public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
        public LocalDateTime getEndsAt() { return endsAt; }
        public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
        public Double getContractValueLKR() { return contractValueLKR; }
        public void setContractValueLKR(Double contractValueLKR) { this.contractValueLKR = contractValueLKR; }
        public boolean isActivate() { return activate; }
        public void setActivate(boolean activate) { this.activate = activate; }
    }

    public static class IssueInvoiceRequest {
        private Double amountLKR;
        private LocalDateTime dueAt;
        private String description;

        public Double getAmountLKR() { return amountLKR; }
        public void setAmountLKR(Double amountLKR) { this.amountLKR = amountLKR; }
        public LocalDateTime getDueAt() { return dueAt; }
        public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class RecordInvoicePaymentRequest {
        @NotBlank(message = "Payment reference is required")
        private String paymentReference;
        private String paymentMethod = "BANK_TRANSFER";
        private LocalDateTime paidAt;

        public String getPaymentReference() { return paymentReference; }
        public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    }

    public static class CancelInvoiceRequest {
        @NotBlank(message = "Reason is required")
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public record BannerDto(
            Long agreementId,
            SponsorshipTier tier,
            String sponsorName,
            String logoUrl,
            String bannerImageUrl,
            String clickThroughUrl,
            int slots
    ) {}

    public record ExposureRoundStats(
            String roundName,
            long impressions,
            long clicks
    ) {}

    public record ExposureReportDto(
            String agreementNo,
            String sponsor,
            SponsorshipTier tier,
            double contractValueLKR,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            long delivered,
            int guaranteed,
            Double fulfilment,
            long clicks,
            double clickThroughRate,
            Double costPerImpressionLKR,
            Map<String, Map<String, Long>> byRound
    ) {
        public long deliveredImpressions() { return delivered; }
        public Double fulfilmentRate() { return fulfilment; }
    }

    public record FinancialsDto(
            String agreementNo,
            String sponsor,
            SponsorshipTier tier,
            double contractValueLKR,
            double invoicedLKR,
            double collectedLKR,
            double outstandingLKR,
            double overdueLKR,
            double uninvoicedLKR,
            List<SponsorInvoice> invoices
    ) {
        public double unbilledLKR() { return uninvoicedLKR; }
    }

    public record LapsedSweepResult(
            int expired,
            List<String> agreements,
            int invoicesMarkedOverdue
    ) {}
}
