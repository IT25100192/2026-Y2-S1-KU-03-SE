package com.starvoicelanka.contestant.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rounds", indexes = {
        @Index(name = "idx_round_season_seq", columnList = "season_id, sequence", unique = true)
})
public class Round extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoundStatus status = RoundStatus.DRAFT;

    @Column(name = "opens_at")
    private LocalDateTime opensAt;

    @Column(name = "closes_at")
    private LocalDateTime closesAt;

    @Column(name = "advance_count", nullable = false)
    private int advanceCount = 0;

    public Round() {}

    public Round(Season season, String name, int sequence) {
        this(season, name, sequence, null, null, 0);
    }

    public Round(Season season, String name, int sequence, LocalDateTime opensAt, LocalDateTime closesAt, int advanceCount) {
        this.season = season;
        this.name = name;
        this.sequence = sequence;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.advanceCount = advanceCount;
        this.status = RoundStatus.DRAFT;
    }

    public boolean isVotingOpen() {
        return isVotingOpen(LocalDateTime.now());
    }

    public boolean isVotingOpen(LocalDateTime now) {
        if (this.status != RoundStatus.OPEN) return false;
        if (this.opensAt != null && now.isBefore(this.opensAt)) return false;
        if (this.closesAt != null && now.isAfter(this.closesAt)) return false;
        return true;
    }

    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public RoundStatus getStatus() { return status; }
    public void setStatus(RoundStatus status) { this.status = status; }

    public LocalDateTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalDateTime opensAt) { this.opensAt = opensAt; }

    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }

    public int getAdvanceCount() { return advanceCount; }
    public void setAdvanceCount(int advanceCount) { this.advanceCount = advanceCount; }
}
