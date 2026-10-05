package com.starvoicelanka.voting.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.RoundStatus;
import com.starvoicelanka.contestant.repository.ContestantRepository;
import com.starvoicelanka.contestant.repository.RoundRepository;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.sponsor.entity.ImpressionPlacement;
import com.starvoicelanka.sponsor.service.SponsorService;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import com.starvoicelanka.user.repository.UserRepository;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;
import com.starvoicelanka.voting.entity.VoteQuota;
import com.starvoicelanka.voting.entity.VoteType;
import com.starvoicelanka.voting.repository.VoteQuotaRepository;
import com.starvoicelanka.voting.repository.VoteRepository;
import com.starvoicelanka.voting.service.fraud.FraudRule;
import com.starvoicelanka.voting.service.fraud.FreshAccountRule;
import com.starvoicelanka.voting.service.fraud.SharedIpRule;
import com.starvoicelanka.voting.service.fraud.VoteBurstRule;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class VotingService {

    private static final Logger log = LoggerFactory.getLogger(VotingService.class);

    private final VoteRepository voteRepository;
    private final VoteQuotaRepository quotaRepository;
    private final ContestantService contestantService;
    private final ContestantRepository contestantRepository;
    private final RoundRepository roundRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final SponsorService sponsorService;
    private final UserRepository userRepository;
    private final AppProperties properties;

    // Strategy pattern: the family of fraud-detection algorithms (Context = this service)
    private final List<FraudRule> fraudRules;

    // Rate limiter cache: userId -> list of submission timestamps
    private final ConcurrentHashMap<Long, List<Long>> rateLimitMap = new ConcurrentHashMap<>();

    public VotingService(VoteRepository voteRepository,
                         VoteQuotaRepository quotaRepository,
                         ContestantService contestantService,
                         ContestantRepository contestantRepository,
                         RoundRepository roundRepository,
                         PaymentService paymentService,
                         NotificationService notificationService,
                         SponsorService sponsorService,
                         UserRepository userRepository,
                         AppProperties properties) {
        this.voteRepository = voteRepository;
        this.quotaRepository = quotaRepository;
        this.contestantService = contestantService;
        this.contestantRepository = contestantRepository;
        this.roundRepository = roundRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.sponsorService = sponsorService;
        this.userRepository = userRepository;
        this.properties = properties;
        this.fraudRules = List.of(
                new SharedIpRule(properties),
                new VoteBurstRule(properties),
                new FreshAccountRule(properties));
    }

    /* ------------------------------------------------------------------ */
    /* VM02 - Eligibility and quota                                        */
    /* ------------------------------------------------------------------ */

    public VoteQuota getQuota(Long voterId, Long roundId) {
        return quotaRepository.findByVoterIdAndRoundId(voterId, roundId)
                .orElseGet(() -> {
                    User voter = userRepository.findById(voterId)
                            .orElseThrow(() -> new ResourceNotFoundException("Voter not found: " + voterId));
                    Round round = roundRepository.findById(roundId)
                            .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
                    VoteQuota quota = new VoteQuota(voter, round);
                    return quotaRepository.save(quota);
                });
    }

    @Transactional(readOnly = true)
    public VotingDtos.EligibilityDto checkEligibility(Long voterId, Long roundId, int count) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
        VoteQuota quota = quotaRepository.findByVoterIdAndRoundId(voterId, roundId)
                .orElse(new VoteQuota());
        CreditAccount account = paymentService.getAccount(voterId);

        int freeRemaining = Math.max(0, properties.getFreeVotesPerRound() - quota.getFreeVotesUsed());
        int freeToUse = Math.min(freeRemaining, count);
        int paidToUse = count - freeToUse;

        boolean canVote = round.isVotingOpen() && (paidToUse == 0 || account.getBalance() >= paidToUse);
        int shortfall = Math.max(0, paidToUse - account.getBalance());

        ContestantDtos.RoundDto roundDto = ContestantDtos.RoundDto.from(round);

        return new VotingDtos.EligibilityDto(
                roundDto,
                count,
                freeRemaining,
                freeToUse,
                paidToUse,
                account.getBalance(),
                canVote,
                shortfall,
                properties.getVotePriceLKR()
        );
    }

    /* ------------------------------------------------------------------ */
    /* VM01 - Cast a Vote                                                  */
    /* ------------------------------------------------------------------ */

    public Map<String, Object> castVote(Long voterId, Long roundId, Long contestantId, int count, String ipAddress, String userAgent) {
        InputValidator.requireId(roundId, "round");
        InputValidator.requireId(contestantId, "contestant");
        // keep headers inside the column sizes so a long value cannot break the save
        if (userAgent != null && userAgent.length() > 300) userAgent = userAgent.substring(0, 300);
        if (ipAddress != null && ipAddress.length() > 60) ipAddress = ipAddress.substring(0, 60);
        if (count < 1 || count > 50) {
            throw new BadRequestException("You can cast between 1 and 50 votes at a time");
        }

        // Rate limiter check per voter: max submissions per minute
        checkRateLimit(voterId);

        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found: " + voterId));

        // 1. Contestant Management: round open, contestant in line-up
        contestantService.assertVotable(roundId, contestantId);

        // 2. Free / paid split
        VoteQuota quota = getQuota(voterId, roundId);
        int freeRemaining = Math.max(0, properties.getFreeVotesPerRound() - quota.getFreeVotesUsed());
        int freeToUse = Math.min(freeRemaining, count);
        int paidToUse = count - freeToUse;

        // 3. Payment Management: take credits before writing votes
        if (paidToUse > 0) {
            paymentService.consumeCredits(voterId, paidToUse);
        }

        Round round = roundRepository.findById(roundId).orElseThrow();
        Contestant contestant = contestantRepository.findById(contestantId).orElseThrow();

        List<Vote> created = new ArrayList<>();
        try {
            if (freeToUse > 0) {
                Vote freeVote = new Vote(voter, round, contestant, VoteType.FREE, freeToUse, 0, ipAddress, userAgent);
                created.add(voteRepository.save(freeVote));
            }
            if (paidToUse > 0) {
                Vote paidVote = new Vote(voter, round, contestant, VoteType.PAID, paidToUse, paidToUse, ipAddress, userAgent);
                created.add(voteRepository.save(paidVote));
            }

            // 4. Increment tallies
            contestantService.addVotes(roundId, contestantId, count);

            quota.setFreeVotesUsed(quota.getFreeVotesUsed() + freeToUse);
            quota.setPaidVotesUsed(quota.getPaidVotesUsed() + paidToUse);
            quotaRepository.save(quota);
        } catch (Exception e) {
            if (paidToUse > 0) {
                paymentService.refundCredits(voterId, paidToUse);
            }
            for (Vote v : created) {
                voteRepository.delete(v);
            }
            throw e;
        }

        // 5. Notification
        int freeLeft = Math.max(0, properties.getFreeVotesPerRound() - quota.getFreeVotesUsed());
        String contestantName = contestant.getDisplayName();

        notificationService.dispatch(voterId, NotificationTemplate.VOTE_CONFIRMATION, Map.of(
                "voteCount", count,
                "voteType", paidToUse > 0 ? "PAID" : "FREE",
                "contestantName", contestantName,
                "roundName", round.getName(),
                "freeVotesLeft", freeLeft
        ));

        return Map.of(
                "accepted", count,
                "freeVotesUsed", freeToUse,
                "paidVotesUsed", paidToUse,
                "freeVotesLeft", freeLeft,
                "contestant", contestantName,
                "round", round.getName()
        );
    }

    private void checkRateLimit(Long voterId) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        List<Long> timestamps = rateLimitMap.compute(voterId, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.removeIf(t -> t < windowStart);
            return v;
        });

        synchronized (timestamps) {
            if (timestamps.size() >= properties.getVoteRateLimitPerMinute()) {
                throw new BadRequestException(String.format(
                        "That is more than %d submissions in a minute. Slow down and try again shortly.",
                        properties.getVoteRateLimitPerMinute()));
            }
            timestamps.add(now);
        }
    }

    /* ------------------------------------------------------------------ */
    /* VM03 - Live tally                                                   */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public VotingDtos.RoundTallyDto getRoundTally(Long roundId) {
        ContestantDtos.LeaderboardDto leaderboard = contestantService.getLeaderboard(roundId);
        List<Vote> votes = voteRepository.findByRoundIdAndVoidedFalse(roundId);

        int totalVotes = 0;
        int freeVotes = 0;
        int paidVotes = 0;
        Set<Long> uniqueVoters = new HashSet<>();

        for (Vote v : votes) {
            totalVotes += v.getCount();
            if (v.getVoteType() == VoteType.FREE) {
                freeVotes += v.getCount();
            } else if (v.getVoteType() == VoteType.PAID) {
                paidVotes += v.getCount();
            }
            if (v.getVoter() != null) {
                uniqueVoters.add(v.getVoter().getId());
            }
        }

        VotingDtos.VoteBreakdownDto breakdown = new VotingDtos.VoteBreakdownDto(
                totalVotes, freeVotes, paidVotes, uniqueVoters.size()
        );

        double revenueLKR = paidVotes * properties.getVotePriceLKR();
        List<com.starvoicelanka.sponsor.dto.SponsorDtos.BannerDto> sponsors =
                sponsorService.getBannersForRound(roundId, ImpressionPlacement.LEADERBOARD);

        return new VotingDtos.RoundTallyDto(
                leaderboard.getRound(),
                totalVotes,
                leaderboard.getStandings(),
                breakdown,
                revenueLKR,
                sponsors
        );
    }

    /* ------------------------------------------------------------------ */
    /* VM04 - Open and close a voting round                                */
    /* ------------------------------------------------------------------ */

    public Round openRound(Long roundId, boolean announce) {
        Round round = contestantService.setRoundStatus(roundId, RoundStatus.OPEN);

        if (announce) {
            ContestantDtos.LeaderboardDto leaderboard = contestantService.getLeaderboard(roundId);
            String closesAt = round.getClosesAt() != null ? round.getClosesAt().toString() : "the end of the show";
            notificationService.broadcast(NotificationTemplate.ROUND_OPENED, Map.of(
                    "roundName", round.getName(),
                    "closesAt", closesAt,
                    "contestantCount", leaderboard.getStandings().size()
            ));
        }
        return round;
    }

    public Round closeRound(Long roundId) {
        return contestantService.setRoundStatus(roundId, RoundStatus.CLOSED);
    }

    /* ------------------------------------------------------------------ */
    /* VM05 - Voting history                                               */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public Page<Vote> listVotes(Long voterId, Long roundId, Pageable pageable) {
        Specification<Vote> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("voided")));
            if (voterId != null) {
                predicates.add(cb.equal(root.get("voter").get("id"), voterId));
            }
            if (roundId != null) {
                predicates.add(cb.equal(root.get("round").get("id"), roundId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return voteRepository.findAll(spec, pageable);
    }

    public Vote voidVote(Long voteId, String reason) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found: " + voteId));

        if (vote.isVoided()) {
            throw new BadRequestException("That vote is already voided");
        }
        reason = InputValidator.optionalText(reason, "Void reason", 500);

        vote.setVoided(true);
        vote.setVoidReason(reason);
        vote = voteRepository.save(vote);

        // Deduct from round tallies
        contestantService.addVotes(vote.getRound().getId(), vote.getContestant().getId(), -vote.getCount());
        return vote;
    }

    /* ------------------------------------------------------------------ */
    /* VM07 - Update / delete votes (change, withdraw, admin remove)       */
    /* ------------------------------------------------------------------ */

    /** UPDATE: a voter moves one of their own votes to another contestant while the round is open. */
    public Vote changeVote(Long voterId, Long voteId, Long newContestantId) {
        InputValidator.requireId(newContestantId, "contestant");
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found: " + voteId));
        if (!vote.getVoter().getId().equals(voterId)) {
            throw new com.starvoicelanka.common.exception.ForbiddenException("You can only change your own votes");
        }
        if (vote.isVoided()) {
            throw new BadRequestException("That vote has been withdrawn and cannot be changed");
        }
        Long roundId = vote.getRound().getId();
        Long oldContestantId = vote.getContestant().getId();
        if (oldContestantId.equals(newContestantId)) {
            throw new BadRequestException("Pick a different contestant to change this vote");
        }
        // Round must still be open and the new contestant must be in the line-up
        contestantService.assertVotable(roundId, newContestantId);
        Contestant newContestant = contestantRepository.findById(newContestantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contestant not found: " + newContestantId));

        contestantService.addVotes(roundId, oldContestantId, -vote.getCount());
        vote.setContestant(newContestant);
        vote = voteRepository.save(vote);
        contestantService.addVotes(roundId, newContestantId, vote.getCount());
        return vote;
    }

    /** DELETE (voter): withdraw an own vote while the round is open; credits and free quota are returned. */
    public Vote retractVote(Long voterId, Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found: " + voteId));
        if (!vote.getVoter().getId().equals(voterId)) {
            throw new com.starvoicelanka.common.exception.ForbiddenException("You can only withdraw your own votes");
        }
        if (vote.isVoided()) {
            throw new BadRequestException("That vote has already been withdrawn");
        }
        if (!vote.getRound().isVotingOpen()) {
            throw new BadRequestException("Votes can only be withdrawn while the round is open");
        }

        vote.setVoided(true);
        vote.setVoidReason("Withdrawn by voter");
        vote = voteRepository.save(vote);
        contestantService.addVotes(vote.getRound().getId(), vote.getContestant().getId(), -vote.getCount());

        VoteQuota quota = getQuota(voterId, vote.getRound().getId());
        if (vote.getVoteType() == VoteType.PAID) {
            if (vote.getCreditsSpent() > 0) {
                paymentService.refundCredits(voterId, vote.getCreditsSpent());
            }
            quota.setPaidVotesUsed(Math.max(0, quota.getPaidVotesUsed() - vote.getCount()));
        } else {
            quota.setFreeVotesUsed(Math.max(0, quota.getFreeVotesUsed() - vote.getCount()));
        }
        quotaRepository.save(quota);
        return vote;
    }

    /** READ: a single vote by id. */
    @Transactional(readOnly = true)
    public Vote getVote(Long voteId) {
        return voteRepository.findById(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found: " + voteId));
    }

    /** READ (admin): every vote including voided ones, newest first. */
    @Transactional(readOnly = true)
    public Page<Vote> listAllVotes(Pageable pageable) {
        return voteRepository.findAll(pageable);
    }

    /** DELETE (admin): permanently remove a vote record; the tally is corrected if it was still counted. */
    public void deleteVote(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found: " + voteId));
        if (!vote.isVoided()) {
            contestantService.addVotes(vote.getRound().getId(), vote.getContestant().getId(), -vote.getCount());
        }
        voteRepository.delete(vote);
    }

    /* ------------------------------------------------------------------ */
    /* VM06 - Fraud detection                                              */
    /* ------------------------------------------------------------------ */

    public VotingDtos.AnomalyReportDto detectAnomalies(Long roundId, boolean autoVoid, boolean notifyAdmins, Long actorId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));

        List<Vote> votes = voteRepository.findWithVoterByRoundIdAndVoidedFalse(roundId);

        List<VotingDtos.FraudFlagDto> flags = new ArrayList<>();

        // STRATEGY PATTERN: every rule is a FraudRule strategy; the service just runs them in turn
        for (FraudRule rule : fraudRules) {
            flags.addAll(rule.check(votes));
        }

        Set<Long> flaggedVoteIds = new LinkedHashSet<>();
        for (VotingDtos.FraudFlagDto flag : flags) {
            flaggedVoteIds.addAll(flag.getVoteIds());
        }

        List<Long> voidedIds = new ArrayList<>();
        if (autoVoid && !flaggedVoteIds.isEmpty()) {
            Map<Long, List<String>> rulesForVote = new HashMap<>();
            for (VotingDtos.FraudFlagDto f : flags) {
                for (Long vid : f.getVoteIds()) {
                    rulesForVote.computeIfAbsent(vid, k -> new ArrayList<>()).add(f.getRule());
                }
            }
            for (Long vid : flaggedVoteIds) {
                try {
                    voidVote(vid, "Fraud sweep: " + String.join(", ", rulesForVote.getOrDefault(vid, List.of("SUSPICIOUS"))));
                    voidedIds.add(vid);
                } catch (Exception e) {
                    // Already voided or removed
                }
            }
        }

        VotingDtos.AnomalyReportDto report = new VotingDtos.AnomalyReportDto(
                ContestantDtos.RoundDto.from(round),
                LocalDateTime.now(),
                Map.of(
                        "sharedIpVoters", properties.getFraudSharedIpVoters(),
                        "burstPerMinute", properties.getFraudBurstPerMinute(),
                        "freshAccountSeconds", properties.getFraudFreshAccountSeconds()
                ),
                flags,
                flaggedVoteIds.size(),
                voidedIds.size(),
                actorId
        );

        if (notifyAdmins && !flags.isEmpty()) {
            raiseAdminAlert(report);
        }

        return report;
    }

    private void raiseAdminAlert(VotingDtos.AnomalyReportDto report) {
        List<User> admins = userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
        long distinctRules = report.getFlags().stream().map(VotingDtos.FraudFlagDto::getRule).distinct().count();

        for (User admin : admins) {
            notificationService.dispatch(admin.getId(), NotificationTemplate.FRAUD_ALERT, Map.of(
                    "roundName", report.getRound().getName(),
                    "flagged", report.getFlaggedVotes(),
                    "rules", distinctRules,
                    "voided", report.getVoidedVotes()
            ));
        }
    }
}
