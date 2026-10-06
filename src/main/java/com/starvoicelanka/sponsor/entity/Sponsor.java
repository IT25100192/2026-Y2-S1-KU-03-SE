package com.starvoicelanka.sponsor.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "sponsors")
public class Sponsor extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String companyName;

    @Column(length = 100)
    private String industry;

    @Column(length = 255)
    private String website;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 150)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_id")
    private User accountManager;

    @Column(nullable = false)
    private boolean isActive = true;

    public Sponsor() {}

    public Sponsor(String companyName, String contactName, String contactEmail) {
        this.companyName = companyName;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.isActive = true;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public User getAccountManager() {
        return accountManager;
    }

    public void setAccountManager(User accountManager) {
        this.accountManager = accountManager;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
