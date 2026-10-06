package com.starvoicelanka.sponsor.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.contestant.entity.Round;
import jakarta.persistence.*;

@Entity
@Table(name = "sponsor_impressions", indexes = {
        @Index(name = "idx_impression_agreement_created", columnList = "agreement_id, created_at")
})
public class SponsorImpression extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private SponsorshipAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImpressionPlacement placement = ImpressionPlacement.LEADERBOARD;

    @Column(nullable = false)
    private boolean clicked = false;

    public SponsorImpression() {}

    public SponsorImpression(SponsorshipAgreement agreement, Round round, ImpressionPlacement placement) {
        this.agreement = agreement;
        this.round = round;
        this.placement = placement != null ? placement : ImpressionPlacement.LEADERBOARD;
        this.clicked = false;
    }

    public SponsorshipAgreement getAgreement() {
        return agreement;
    }

    public void setAgreement(SponsorshipAgreement agreement) {
        this.agreement = agreement;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public ImpressionPlacement getPlacement() {
        return placement;
    }

    public void setPlacement(ImpressionPlacement placement) {
        this.placement = placement;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }
}
