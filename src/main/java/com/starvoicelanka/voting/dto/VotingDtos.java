package com.starvoicelanka.voting.dto;

import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.voting.entity.Vote;
import com.starvoicelanka.voting.entity.VoteType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class VotingDtos {

    public static class CastVoteRequest {
        @NotNull(message = "Round ID is required")
        private Long roundId;

        @NotNull(message = "Contestant ID is required")
        private Long contestantId;

        @Min(value = 1, message = "Must cast at least 1 vote")
        @Max(value = 50, message = "Cannot cast more than 50 votes at a time")
        private int count = 1;

        public Long getRoundId() { return roundId; }
        public void setRoundId(Long roundId) { this.roundId = roundId; }
        public Long getContestantId() { return contestantId; }
        public void setContestantId(Long contestantId) { this.contestantId = contestantId; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class EligibilityDto {
        private ContestantDtos.RoundDto round;
        private int requested;
        private int freeVotesRemaining;
        private int freeVotesToUse;
        private int paidVotesToUse;
        private int creditBalance;
        private boolean canVote;
        private int shortfall;
        private double pricePerVoteLKR;

        public EligibilityDto(ContestantDtos.RoundDto round, int requested, int freeVotesRemaining,
                              int freeVotesToUse, int paidVotesToUse, int creditBalance,
                              boolean canVote, int shortfall, double pricePerVoteLKR) {
            this.round = round;
            this.requested = requested;
            this.freeVotesRemaining = freeVotesRemaining;
            this.freeVotesToUse = freeVotesToUse;
            this.paidVotesToUse = paidVotesToUse;
            this.creditBalance = creditBalance;
            this.canVote = canVote;
            this.shortfall = shortfall;
            this.pricePerVoteLKR = pricePerVoteLKR;
        }

        public ContestantDtos.RoundDto getRound() { return round; }
        public int getRequested() { return requested; }
        public int getFreeVotesRemaining() { return freeVotesRemaining; }
        public int getFreeVotesToUse() { return freeVotesToUse; }
        public int getPaidVotesToUse() { return paidVotesToUse; }
        public int getCreditBalance() { return creditBalance; }
        public boolean isCanVote() { return canVote; }
        public int getShortfall() { return shortfall; }
        public double getPricePerVoteLKR() { return pricePerVoteLKR; }
    }

    public static class VoteBreakdownDto {
        private int totalVotes;
        private int freeVotes;
        private int paidVotes;
        private int uniqueVoters;

        public VoteBreakdownDto(int totalVotes, int freeVotes, int paidVotes, int uniqueVoters) {
            this.totalVotes = totalVotes;
            this.freeVotes = freeVotes;
            this.paidVotes = paidVotes;
            this.uniqueVoters = uniqueVoters;
        }

        public int getTotalVotes() { return totalVotes; }
        public int getFreeVotes() { return freeVotes; }
        public int getPaidVotes() { return paidVotes; }
        public int getUniqueVoters() { return uniqueVoters; }
    }

    public static class RoundTallyDto {
        private ContestantDtos.RoundDto round;
        private int totalVotes;
        private List<ContestantDtos.LeaderboardStandingDto> standings;
        private VoteBreakdownDto breakdown;
        private double revenueLKR;
        private Object sponsors;

        public RoundTallyDto(ContestantDtos.RoundDto round, int totalVotes,
                             List<ContestantDtos.LeaderboardStandingDto> standings,
                             VoteBreakdownDto breakdown, double revenueLKR, Object sponsors) {
            this.round = round;
            this.totalVotes = totalVotes;
            this.standings = standings;
            this.breakdown = breakdown;
            this.revenueLKR = revenueLKR;
            this.sponsors = sponsors;
        }

        public ContestantDtos.RoundDto getRound() { return round; }
        public int getTotalVotes() { return totalVotes; }
        public List<ContestantDtos.LeaderboardStandingDto> getStandings() { return standings; }
        public VoteBreakdownDto getBreakdown() { return breakdown; }
        public double getRevenueLKR() { return revenueLKR; }
        public Object getSponsors() { return sponsors; }
    }

    public static class VoteDto {
        private Long id;
        private Long voterId;
        private Long roundId;
        private String roundName;
        private Long contestantId;
        private String contestantName;
        private VoteType voteType;
        private int count;
        private int creditsSpent;
        private boolean voided;
        private String voidReason;
        private LocalDateTime createdAt;

        public static VoteDto from(Vote v) {
            if (v == null) return null;
            VoteDto dto = new VoteDto();
            dto.id = v.getId();
            dto.voterId = v.getVoter() != null ? v.getVoter().getId() : null;
            dto.roundId = v.getRound() != null ? v.getRound().getId() : null;
            dto.roundName = v.getRound() != null ? v.getRound().getName() : null;
            dto.contestantId = v.getContestant() != null ? v.getContestant().getId() : null;
            dto.contestantName = v.getContestant() != null ? v.getContestant().getDisplayName() : null;
            dto.voteType = v.getVoteType();
            dto.count = v.getCount();
            dto.creditsSpent = v.getCreditsSpent();
            dto.voided = v.isVoided();
            dto.voidReason = v.getVoidReason();
            dto.createdAt = v.getCreatedAt();
            return dto;
        }

        public Long getId() { return id; }
        public Long getVoterId() { return voterId; }
        public Long getRoundId() { return roundId; }
        public String getRoundName() { return roundName; }
        public Long getContestantId() { return contestantId; }
        public String getContestantName() { return contestantName; }
        public VoteType getVoteType() { return voteType; }
        public int getCount() { return count; }
        public int getCreditsSpent() { return creditsSpent; }
        public boolean isVoided() { return voided; }
        public String getVoidReason() { return voidReason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class FraudFlagDto {
        private String rule;
        private String severity;
        private String subject;
        private String description;
        private List<Long> voterIds;
        private List<Long> voteIds;
        private int votes;

        public FraudFlagDto(String rule, String severity, String subject, String description, List<Long> voterIds, List<Long> voteIds, int votes) {
            this.rule = rule;
            this.severity = severity;
            this.subject = subject;
            this.description = description;
            this.voterIds = voterIds;
            this.voteIds = voteIds;
            this.votes = votes;
        }

        public String getRule() { return rule; }
        public String getSeverity() { return severity; }
        public String getSubject() { return subject; }
        public String getDescription() { return description; }
        public List<Long> getVoterIds() { return voterIds; }
        public List<Long> getVoteIds() { return voteIds; }
        public int getVotes() { return votes; }
    }

    public static class AnomalyReportDto {
        private ContestantDtos.RoundDto round;
        private LocalDateTime ranAt;
        private Object thresholds;
        private List<FraudFlagDto> flags;
        private int flaggedVotes;
        private int voidedVotes;
        private Long actorId;

        public AnomalyReportDto(ContestantDtos.RoundDto round, LocalDateTime ranAt, Object thresholds, List<FraudFlagDto> flags, int flaggedVotes, int voidedVotes, Long actorId) {
            this.round = round;
            this.ranAt = ranAt;
            this.thresholds = thresholds;
            this.flags = flags;
            this.flaggedVotes = flaggedVotes;
            this.voidedVotes = voidedVotes;
            this.actorId = actorId;
        }

        public ContestantDtos.RoundDto getRound() { return round; }
        public LocalDateTime getRanAt() { return ranAt; }
        public Object getThresholds() { return thresholds; }
        public List<FraudFlagDto> getFlags() { return flags; }
        public int getFlaggedVotes() { return flaggedVotes; }
        public int getVoidedVotes() { return voidedVotes; }
        public Long getActorId() { return actorId; }
    }
}
