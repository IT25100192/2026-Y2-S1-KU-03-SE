package com.starvoicelanka.sponsor.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "sponsorship_packages")
public class SponsorshipPackage extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private SponsorshipTier tier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private double priceLKR;

    @Column(nullable = false)
    private int guaranteedImpressions = 0;

    @Column(nullable = false)
    private int bannerSlotsPerRound = 1;

    @Column(nullable = false)
    private boolean logoOnLeaderboard = false;

    @Column(nullable = false)
    private boolean namingRights = false;

    public SponsorshipPackage() {}

    public SponsorshipPackage(SponsorshipTier tier, String name, double priceLKR, int guaranteedImpressions, int bannerSlotsPerRound, boolean logoOnLeaderboard, boolean namingRights) {
        this.tier = tier;
        this.name = name;
        this.priceLKR = priceLKR;
        this.guaranteedImpressions = guaranteedImpressions;
        this.bannerSlotsPerRound = bannerSlotsPerRound;
        this.logoOnLeaderboard = logoOnLeaderboard;
        this.namingRights = namingRights;
    }

    public SponsorshipTier getTier() {
        return tier;
    }

    public void setTier(SponsorshipTier tier) {
        this.tier = tier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPriceLKR() {
        return priceLKR;
    }

    public void setPriceLKR(double priceLKR) {
        this.priceLKR = priceLKR;
    }

    public int getGuaranteedImpressions() {
        return guaranteedImpressions;
    }

    public void setGuaranteedImpressions(int guaranteedImpressions) {
        this.guaranteedImpressions = guaranteedImpressions;
    }

    public int getBannerSlotsPerRound() {
        return bannerSlotsPerRound;
    }

    public void setBannerSlotsPerRound(int bannerSlotsPerRound) {
        this.bannerSlotsPerRound = bannerSlotsPerRound;
    }

    public boolean isLogoOnLeaderboard() {
        return logoOnLeaderboard;
    }

    public void setLogoOnLeaderboard(boolean logoOnLeaderboard) {
        this.logoOnLeaderboard = logoOnLeaderboard;
    }

    public boolean isNamingRights() {
        return namingRights;
    }

    public void setNamingRights(boolean namingRights) {
        this.namingRights = namingRights;
    }
}
