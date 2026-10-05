package com.starvoicelanka.voting.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_vote_round_contestant", columnList = "round_id, contestant_id, created_at"),
        @Index(name = "idx_vote_voter_round", columnList = "voter_id, round_id"),
        @Index(name = "idx_vote_ip", columnList = "ip_address")
})
public class Vote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", nullable = false)
    private Contestant contestant;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 20)
    private VoteType voteType;

    @Column(nullable = false)
    private int count;

    @Column(name = "credits_spent", nullable = false)
    private int creditsSpent = 0;

    @Column(nullable = false)
    private boolean voided = false;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    public Vote() {}

    public Vote(User voter, Round round, Contestant contestant, VoteType voteType, int count, int creditsSpent, String ipAddress, String userAgent) {
        this.voter = voter;
        this.round = round;
        this.contestant = contestant;
        this.voteType = voteType;
        this.count = count;
        this.creditsSpent = creditsSpent;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.voided = false;
    }

    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }

    public Round getRound() { return round; }
    public void setRound(Round round) { this.round = round; }

    public Contestant getContestant() { return contestant; }
    public void setContestant(Contestant contestant) { this.contestant = contestant; }

    public VoteType getVoteType() { return voteType; }
    public void setVoteType(VoteType voteType) { this.voteType = voteType; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getCreditsSpent() { return creditsSpent; }
    public void setCreditsSpent(int creditsSpent) { this.creditsSpent = creditsSpent; }

    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }

    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
