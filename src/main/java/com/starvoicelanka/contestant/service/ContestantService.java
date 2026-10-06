package com.starvoicelanka.contestant.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.*;
import com.starvoicelanka.contestant.repository.*;
import com.starvoicelanka.contestant.service.transition.RoundTransitionFactory;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.sponsor.repository.SponsorImpressionRepository;
import com.starvoicelanka.voting.repository.VoteQuotaRepository;
import com.starvoicelanka.voting.repository.VoteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ContestantService {

    private final SeasonRepository seasonRepository;
    private final RoundRepository roundRepository;
    private final ContestantRepository contestantRepository;
    private final RoundEntryRepository roundEntryRepository;
    private final MediaStorageService mediaStorageService;
    private final NotificationService notificationService;
    private final RoundTransitionFactory transitionFactory = new RoundTransitionFactory();

    @Autowired(required = false)
    private VoteRepository voteRepository;
    @Autowired(required = false)
    private VoteQuotaRepository voteQuotaRepository;
    @Autowired(required = false)
    private SponsorImpressionRepository sponsorImpressionRepository;

    public ContestantService(SeasonRepository seasonRepository,
                             RoundRepository roundRepository,
                             ContestantRepository contestantRepository,
                             RoundEntryRepository roundEntryRepository,
                             MediaStorageService mediaStorageService,
                             NotificationService notificationService) {
        this.seasonRepository = seasonRepository;
        this.roundRepository = roundRepository;
        this.contestantRepository = contestantRepository;
        this.roundEntryRepository = roundEntryRepository;
        this.mediaStorageService = mediaStorageService;
        this.notificationService = notificationService;
    }

    /* ---- Seasons --------------------------------------------------------- */

    public Season createSeason(String name, int year, boolean isCurrent) {
        name = InputValidator.requireText(name, "Season name", 2, 120);
        InputValidator.requireRange(year, "Season year", 2000, 2100);
        if (isCurrent) {
            seasonRepository.unsetCurrentSeasons();
        }
        Season season = new Season(name, year, isCurrent);
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public Season getCurrentSeason() {
        return seasonRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No season is marked as current"));
    }

    @Transactional(readOnly = true)
    public Season getSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));
    }

    @Transactional(readOnly = true)
    public List<Season> listSeasons() {
        List<Season> all = new ArrayList<>(seasonRepository.findAll());
        all.sort((a, b) -> a.getYear() != b.getYear()
                ? Integer.compare(b.getYear(), a.getYear())
                : Long.compare(b.getId(), a.getId()));
        return all;
    }

    @Transactional(readOnly = true)
    public long countRoundsInSeason(Long seasonId) {
        return roundRepository.countBySeasonId(seasonId);
    }

    @Transactional(readOnly = true)
    public long countContestantsInSeason(Long seasonId) {
        return contestantRepository.countBySeasonId(seasonId);
    }

    public Season updateSeason(Long seasonId, String name, int year) {
        Season season = getSeason(seasonId);
        name = InputValidator.requireText(name, "Season name", 2, 120);
        InputValidator.requireRange(year, "Season year", 2000, 2100);
        season.setName(name);
        season.setYear(year);
        return seasonRepository.save(season);
    }

    /** Makes one season the current one. Rounds and contestants screens follow the current season. */
    public Season setCurrentSeason(Long seasonId) {
        Season season = getSeason(seasonId);
        if (season.isCurrent()) {
            return season;
        }
        seasonRepository.findByIsCurrentTrue().ifPresent(current -> {
            boolean hasOpenRound = roundRepository.findBySeasonIdOrderBySequenceAsc(current.getId())
                    .stream().anyMatch(r -> r.getStatus() == RoundStatus.OPEN);
            if (hasOpenRound) {
                throw new ConflictException("Close the open round in '" + current.getName() + "' before switching season");
            }
        });
        seasonRepository.unsetCurrentSeasons();
        season.setCurrent(true);
        return seasonRepository.save(season);
    }

    public void deleteSeason(Long seasonId) {
        Season season = getSeason(seasonId);
        if (season.isCurrent()) {
            throw new ConflictException("Cannot delete the current season. Make another season current first");
        }
        if (roundRepository.countBySeasonId(seasonId) > 0 || contestantRepository.countBySeasonId(seasonId) > 0) {
            throw new ConflictException("Cannot delete a season that still has rounds or contestants");
        }
        seasonRepository.delete(season);
    }

    /* ---- Contestants ----------------------------------------------------- */

    public Contestant registerContestant(ContestantDtos.RegisterContestantRequest data) {
        data.setFullName(InputValidator.requireText(data.getFullName(), "Full name", 2, 120));
        data.setStageName(InputValidator.optionalText(data.getStageName(), "Stage name", 120));
        if (data.getAge() != null) InputValidator.requireRange(data.getAge(), "Age", 5, 100);
        data.setDistrict(InputValidator.requireText(data.getDistrict(), "District", 2, 60));
        data.setBio(InputValidator.optionalText(data.getBio(), "Bio", 1000));
        data.setPhotoUrl(InputValidator.optionalUrl(data.getPhotoUrl(), "Photo link", 255));
        Season season = data.getSeasonId() != null
                ? seasonRepository.findById(data.getSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + data.getSeasonId()))
                : getCurrentSeason();

        if (contestantRepository.findBySeasonIdAndFullName(season.getId(), data.getFullName()).isPresent()) {
            throw new ConflictException("That contestant is already registered for this season");
        }

        Contestant c = new Contestant(
                season,
                data.getFullName(),
                data.getStageName(),
                data.getAge(),
                data.getDistrict(),
                data.getBio(),
                data.getPhotoUrl()
        );
        return contestantRepository.save(c);
    }

    public Contestant updateContestant(Long contestantId, ContestantDtos.UpdateContestantRequest changes) {
        Contestant c = getContestant(contestantId);
        if (changes.getFullName() != null) {
            String newName = InputValidator.requireText(changes.getFullName(), "Full name", 2, 120);
            if (!newName.equalsIgnoreCase(c.getFullName())
                    && contestantRepository.findBySeasonIdAndFullName(c.getSeason().getId(), newName).isPresent()) {
                throw new ConflictException("Another contestant in this season already has that name");
            }
            c.setFullName(newName);
        }
        if (changes.getStageName() != null) c.setStageName(InputValidator.optionalText(changes.getStageName(), "Stage name", 120));
        if (changes.getAge() != null) c.setAge(InputValidator.requireRange(changes.getAge(), "Age", 5, 100));
        if (changes.getDistrict() != null) c.setDistrict(InputValidator.requireText(changes.getDistrict(), "District", 2, 60));
        if (changes.getBio() != null) c.setBio(InputValidator.optionalText(changes.getBio(), "Bio", 1000));
        if (changes.getPhotoUrl() != null) c.setPhotoUrl(InputValidator.optionalUrl(changes.getPhotoUrl(), "Photo link", 255));
        return contestantRepository.save(c);
    }

    public Contestant setContestantStatus(Long contestantId, ContestantStatus status) {
        Contestant c = getContestant(contestantId);
        c.setStatus(status);
        return contestantRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Contestant getContestant(Long contestantId) {
        return contestantRepository.findById(contestantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contestant not found: " + contestantId));
    }

    @Transactional(readOnly = true)
    public Page<Contestant> listContestants(Long seasonId, ContestantStatus status, Pageable pageable) {
        Specification<Contestant> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return contestantRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public long countActiveContestants(Long seasonId) {
        return contestantRepository.countBySeasonIdAndStatus(seasonId, ContestantStatus.ACTIVE);
    }

    public void deleteContestant(Long contestantId) {
        Contestant c = getContestant(contestantId);
        if (voteRepository != null) {
            voteRepository.deleteByContestantId(contestantId);
        }
        roundEntryRepository.deleteByContestantId(contestantId);
        contestantRepository.delete(c);
    }

    /** UPDATE: edit a round's name, order and advance count (only while it is still a draft). */
    public Round updateRound(Long roundId, String name, Integer sequence, Integer advanceCount) {
        Round r = getRound(roundId);
        if (r.getStatus() != RoundStatus.DRAFT) {
            throw new BadRequestException("Only a draft round can be edited");
        }
        if (name != null && !name.isBlank()) r.setName(InputValidator.requireText(name, "Round name", 2, 120));
        if (sequence != null) {
            r.setSequence(InputValidator.requireRange(sequence, "Round order", 1, 100));
        }
        if (advanceCount != null) {
            r.setAdvanceCount(InputValidator.requireRange(advanceCount, "Advance count", 0, 1000));
        }
        return roundRepository.save(r);
    }

    /** DELETE: take one contestant out of a draft round's line-up. */
    public void removeFromLineUp(Long roundId, Long contestantId) {
        Round round = getRound(roundId);
        if (round.getStatus() != RoundStatus.DRAFT) {
            throw new BadRequestException("The line-up can only be changed while the round is a draft");
        }
        RoundEntry entry = roundEntryRepository.findByRoundIdAndContestantId(roundId, contestantId)
                .orElseThrow(() -> new ResourceNotFoundException("That contestant is not in this round"));
        roundEntryRepository.delete(entry);
    }

    public void deleteRound(Long roundId) {
        Round r = getRound(roundId);
        if (voteRepository != null) {
            voteRepository.deleteByRoundId(roundId);
        }
        if (voteQuotaRepository != null) {
            voteQuotaRepository.deleteByRoundId(roundId);
        }
        // Sponsor campaign slots can be scheduled against a round (SP04). A round
        // is only ever referenced optionally from an impression, so detach it
        // rather than delete the sponsor's impression/report data.
        if (sponsorImpressionRepository != null) {
            sponsorImpressionRepository.nullifyRoundId(roundId);
        }
        roundEntryRepository.deleteByRoundId(roundId);
        roundRepository.delete(r);
    }

    /* ---- Rounds & Line-ups ----------------------------------------------- */

    public Round createRound(ContestantDtos.CreateRoundRequest data) {
        data.setName(InputValidator.requireText(data.getName(), "Round name", 2, 120));
        if (data.getSequence() != null) InputValidator.requireRange(data.getSequence(), "Round order", 1, 100);
        if (data.getAdvanceCount() != null) InputValidator.requireRange(data.getAdvanceCount(), "Advance count", 0, 1000);
        InputValidator.requireDateOrder(data.getOpensAt(), data.getClosesAt(), "the opening time", "the closing time");
        Season season = data.getSeasonId() != null
                ? seasonRepository.findById(data.getSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + data.getSeasonId()))
                : getCurrentSeason();

        Round round = new Round(
                season,
                data.getName(),
                data.getSequence(),
                data.getOpensAt(),
                data.getClosesAt(),
                data.getAdvanceCount() != null ? data.getAdvanceCount() : 0
        );
        return roundRepository.save(round);
    }

    @Transactional(readOnly = true)
    public Round getRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
    }

    @Transactional(readOnly = true)
    public List<Round> listRounds(Long seasonId) {
        return roundRepository.findBySeasonIdOrderBySequenceAsc(seasonId);
    }

    public List<RoundEntry> assignContestantsToRound(Long roundId, List<Long> contestantIds) {
        Round round = getRound(roundId);
        if (round.getStatus() != RoundStatus.DRAFT) {
            throw new BadRequestException("The line-up can only be changed while the round is a draft");
        }

        if (contestantIds == null || contestantIds.isEmpty()) {
            throw new BadRequestException("Pick at least one contestant to add");
        }
        List<RoundEntry> entries = new ArrayList<>();
        for (Long contestantId : contestantIds) {
            Contestant contestant = getContestant(contestantId);
            if (contestant.getStatus() == ContestantStatus.ELIMINATED) {
                throw new BadRequestException(contestant.getFullName() + " has already been eliminated");
            }

            RoundEntry entry = roundEntryRepository.findByRoundIdAndContestantId(round.getId(), contestant.getId())
                    .orElseGet(() -> roundEntryRepository.save(new RoundEntry(round, contestant)));
            entries.add(entry);
        }
        return entries;
    }

    public Round setRoundStatus(Long roundId, RoundStatus status) {
        Round round = getRound(roundId);

        // FACTORY PATTERN: the factory returns the rule object for the current status
        boolean allowed = transitionFactory.createTransition(round.getStatus()).canMoveTo(status);

        if (!allowed) {
            throw new BadRequestException("Cannot move a round from " + round.getStatus() + " to " + status);
        }

        if (status == RoundStatus.OPEN) {
            long lineUpCount = roundEntryRepository.countByRoundId(round.getId());
            if (lineUpCount < 2) {
                throw new BadRequestException("A round needs at least two contestants before it opens");
            }
            if (round.getOpensAt() == null) {
                round.setOpensAt(LocalDateTime.now());
            }
        }

        round.setStatus(status);
        return roundRepository.save(round);
    }

    public record VotableInfo(Round round, RoundEntry entry) {}

    @Transactional(readOnly = true)
    public VotableInfo assertVotable(Long roundId, Long contestantId) {
        Round round = getRound(roundId);
        if (!round.isVotingOpen()) {
            throw new BadRequestException("Voting is not open for " + round.getName());
        }

        RoundEntry entry = roundEntryRepository.findByRoundIdAndContestantId(roundId, contestantId)
                .orElseThrow(() -> new BadRequestException("That contestant is not in this round"));

        if (entry.getContestant().getStatus() == ContestantStatus.ELIMINATED) {
            throw new BadRequestException("That contestant has been eliminated");
        }

        return new VotableInfo(round, entry);
    }

    public void addVotes(Long roundId, Long contestantId, int count) {
        roundEntryRepository.incrementVotes(roundId, contestantId, count);
        Contestant c = getContestant(contestantId);
        c.setTotalVotes(c.getTotalVotes() + count);
        contestantRepository.save(c);
    }

    /* ---- CM04 - Leaderboard ---------------------------------------------- */

    @Transactional(readOnly = true)
    public ContestantDtos.LeaderboardDto getLeaderboard(Long roundId) {
        Round round = getRound(roundId);
        List<RoundEntry> entries = roundEntryRepository.findByRoundIdOrderByVotesInRoundDesc(roundId);

        int totalVotes = entries.stream().mapToInt(RoundEntry::getVotesInRound).sum();
        List<ContestantDtos.LeaderboardStandingDto> standings = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            RoundEntry entry = entries.get(i);
            Contestant c = entry.getContestant();
            double share = totalVotes > 0 ? Math.round(((double) entry.getVotesInRound() / totalVotes) * 10000.0) / 100.0 : 0.0;
            standings.add(new ContestantDtos.LeaderboardStandingDto(
                    i + 1,
                    c.getId(),
                    c.getDisplayName(),
                    c.getDistrict(),
                    c.getPhotoUrl(),
                    entry.getVotesInRound(),
                    share,
                    entry.getOutcome()
            ));
        }

        return new ContestantDtos.LeaderboardDto(ContestantDtos.RoundDto.from(round), totalVotes, standings);
    }

    /* ---- CM05 - Publish results ------------------------------------------ */

    public ContestantDtos.LeaderboardDto publishResults(Long roundId) {
        Round round = getRound(roundId);
        if (round.getStatus() != RoundStatus.CLOSED) {
            throw new BadRequestException("Close the round before publishing results");
        }

        List<RoundEntry> entries = roundEntryRepository.findByRoundIdOrderByVotesInRoundDesc(roundId);
        int advanceCount = round.getAdvanceCount() > 0 ? round.getAdvanceCount() : Math.max(1, entries.size() - 1);

        for (int i = 0; i < entries.size(); i++) {
            RoundEntry entry = entries.get(i);
            boolean advanced = i < advanceCount;

            entry.setPosition(i + 1);
            entry.setOutcome(advanced ? RoundOutcome.ADVANCED : RoundOutcome.ELIMINATED);
            roundEntryRepository.save(entry);

            Contestant contestant = entry.getContestant();
            contestant.setStatus(advanced ? ContestantStatus.ACTIVE : ContestantStatus.ELIMINATED);
            contestantRepository.save(contestant);

            // In-app elimination notice
            notificationService.dispatch(null, NotificationTemplate.ELIMINATION_NOTICE, Map.of(
                    "roundName", round.getName(),
                    "contestantName", contestant.getDisplayName(),
                    "totalVotes", entry.getVotesInRound(),
                    "outcome", entry.getOutcome().name()
            ));
        }

        round.setStatus(RoundStatus.RESULTS_PUBLISHED);
        roundRepository.save(round);

        return getLeaderboard(roundId);
    }

    /* ---- CM06 - Performance Media ---------------------------------------- */

    public RoundEntry uploadPerformanceMedia(Long roundId, Long contestantId, MultipartFile file, String performanceTitle) {
        Round round = getRound(roundId);
        if (round.getStatus() == RoundStatus.RESULTS_PUBLISHED) {
            throw new BadRequestException("Results are published for that round, its media is now fixed");
        }

        RoundEntry entry = roundEntryRepository.findByRoundIdAndContestantId(roundId, contestantId)
                .orElseThrow(() -> new BadRequestException("That contestant is not in this round"));

        if (entry.getMediaUrl() != null) {
            mediaStorageService.deleteMedia(entry.getMediaUrl());
        }

        MediaStorageService.StoredMedia stored = mediaStorageService.storeMedia(roundId, contestantId, file);
        entry.setMediaUrl(stored.mediaUrl());
        entry.setMediaMime(stored.mediaMime());
        entry.setMediaSizeBytes(stored.sizeBytes());
        entry.setMediaOriginalName(stored.originalName());
        entry.setMediaUploadedAt(LocalDateTime.now());
        if (performanceTitle != null) {
            entry.setPerformanceTitle(performanceTitle);
        }

        return roundEntryRepository.save(entry);
    }

    public RoundEntry removePerformanceMedia(Long roundId, Long contestantId) {
        RoundEntry entry = roundEntryRepository.findByRoundIdAndContestantId(roundId, contestantId)
                .orElseThrow(() -> new BadRequestException("That contestant is not in this round"));

        if (entry.getMediaUrl() == null) {
            throw new BadRequestException("There is no clip on that entry");
        }

        mediaStorageService.deleteMedia(entry.getMediaUrl());
        entry.setMediaUrl(null);
        entry.setMediaMime(null);
        entry.setMediaSizeBytes(null);
        entry.setMediaOriginalName(null);
        entry.setMediaUploadedAt(null);
        return roundEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<RoundEntry> getRoundLineUp(Long roundId) {
        return roundEntryRepository.findByRoundIdOrderByVotesInRoundDesc(roundId);
    }

    @Transactional(readOnly = true)
    public ContestantDtos.RoundLineUpDto getRoundLineUpDto(Long roundId) {
        Round round = getRound(roundId);
        List<RoundEntry> entries = getRoundLineUp(roundId);
        List<ContestantDtos.RoundEntryDto> dtos = entries.stream().map(ContestantDtos.RoundEntryDto::from).toList();
        return new ContestantDtos.RoundLineUpDto(ContestantDtos.RoundDto.from(round), dtos);
    }

    @Transactional(readOnly = true)
    public List<RoundEntry> getContestantEntries(Long contestantId) {
        return roundEntryRepository.findByContestantId(contestantId);
    }
}
