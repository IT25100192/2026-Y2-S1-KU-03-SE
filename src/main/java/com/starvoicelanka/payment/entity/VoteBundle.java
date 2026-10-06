package com.starvoicelanka.payment.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "vote_bundles")
public class VoteBundle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "vote_credits", nullable = false)
    private int voteCredits;

    @Column(name = "price_lkr", nullable = false)
    private double priceLKR;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public VoteBundle() {}

    public VoteBundle(String code, String name, int voteCredits, double priceLKR, boolean isActive) {
        this.code = code != null ? code.toUpperCase().trim() : null;
        this.name = name != null ? name.trim() : null;
        this.voteCredits = voteCredits;
        this.priceLKR = priceLKR;
        this.isActive = isActive;
    }

    public double getPricePerVote() {
        if (voteCredits <= 0) return 0.0;
        return Math.round((priceLKR / voteCredits) * 100.0) / 100.0;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.toUpperCase().trim() : null; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getVoteCredits() { return voteCredits; }
    public void setVoteCredits(int voteCredits) { this.voteCredits = voteCredits; }

    public double getPriceLKR() { return priceLKR; }
    public void setPriceLKR(double priceLKR) { this.priceLKR = priceLKR; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
