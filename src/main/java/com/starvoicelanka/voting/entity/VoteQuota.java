package com.starvoicelanka.voting.entity;

import com.starvoicelanka.common.BaseEntity;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "vote_quotas", indexes = {
        @Index(name = "idx_quota_voter_round", columnList = "voter_id, round_id", unique = true)
})
public class VoteQuota extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "free_votes_used", nullable = false)
    private int freeVotesUsed = 0;

    @Column(name = "paid_votes_used", nullable = false)
    private int paidVotesUsed = 0;

    public VoteQuota() {}

    public VoteQuota(User voter, Round round) {
        this.voter = voter;
        this.round = round;
        this.freeVotesUsed = 0;
        this.paidVotesUsed = 0;
    }

    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }

    public Round getRound() { return round; }
    public void setRound(Round round) { this.round = round; }

    public int getFreeVotesUsed() { return freeVotesUsed; }
    public void setFreeVotesUsed(int freeVotesUsed) { this.freeVotesUsed = freeVotesUsed; }

    public int getPaidVotesUsed() { return paidVotesUsed; }
    public void setPaidVotesUsed(int paidVotesUsed) { this.paidVotesUsed = paidVotesUsed; }
}
