package com.starvoicelanka.web;

import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.RoundStatus;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.payment.entity.VoteBundle;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.sponsor.dto.SponsorDtos;
import com.starvoicelanka.sponsor.entity.SponsorshipAgreement;
import com.starvoicelanka.sponsor.entity.SponsorshipPackage;
import com.starvoicelanka.sponsor.service.SponsorService;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.service.VotingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;

@Controller
public class SiteWebController {

    private final ContestantService contestantService;
    private final VotingService votingService;
    private final PaymentService paymentService;
    private final SponsorService sponsorService;

    public SiteWebController(ContestantService contestantService,
                             VotingService votingService,
                             PaymentService paymentService,
                             SponsorService sponsorService) {
        this.contestantService = contestantService;
        this.votingService = votingService;
        this.paymentService = paymentService;
        this.sponsorService = sponsorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        Season season = null;
        try {
            season = contestantService.getCurrentSeason();
        } catch (Exception ignored) {}

        List<Round> rounds = season != null ? contestantService.listRounds(season.getId()) : Collections.emptyList();
        Round round = rounds.stream().filter(r -> r.getStatus() == RoundStatus.OPEN).findFirst()
                .orElse(rounds.isEmpty() ? null : rounds.get(0));

        VotingDtos.RoundTallyDto tally = round != null ? votingService.getRoundTally(round.getId()) : null;

        long activeContestants = 0;
        if (season != null) {
            Page<Contestant> activePage = contestantService.listContestants(season.getId(), com.starvoicelanka.contestant.entity.ContestantStatus.ACTIVE, PageRequest.of(0, 1));
            activeContestants = activePage.getTotalElements();
        }

        model.addAttribute("title", "Home");
        model.addAttribute("season", season);
        model.addAttribute("round", round);
        model.addAttribute("standings", tally != null ? tally.getStandings() : Collections.emptyList());
        model.addAttribute("breakdown", tally != null ? tally.getBreakdown() : new VotingDtos.VoteBreakdownDto(0, 0, 0, 0));
        model.addAttribute("sponsors", tally != null ? tally.getSponsors() : Collections.emptyList());
        model.addAttribute("activeContestants", activeContestants);
        return "site/index";
    }

    @GetMapping("/contestants")
    public String contestants(Model model) {
        Season season = null;
        try {
            season = contestantService.getCurrentSeason();
        } catch (Exception ignored) {}

        List<Contestant> contestants = Collections.emptyList();
        if (season != null) {
            Page<Contestant> page = contestantService.listContestants(season.getId(), null, PageRequest.of(0, 100));
            contestants = page.getContent();
        }

        model.addAttribute("title", "Contestants");
        model.addAttribute("season", season);
        model.addAttribute("contestants", contestants);
        return "site/contestants";
    }

    @GetMapping("/contestants/{id}")
    public String contestantDetail(@PathVariable("id") Long id, Model model) {
        Contestant contestant = contestantService.getContestant(id);
        Season season = contestant.getSeason() != null ? contestant.getSeason() : contestantService.getCurrentSeason();
        List<Round> rounds = season != null ? contestantService.listRounds(season.getId()) : Collections.emptyList();

        List<Map<String, Object>> entries = new ArrayList<>();
        for (Round r : rounds) {
            try {
                ContestantDtos.RoundLineUpDto lineUp = contestantService.getRoundLineUpDto(r.getId());
                if (lineUp != null && lineUp.getEntries() != null) {
                    for (ContestantDtos.RoundEntryDto entry : lineUp.getEntries()) {
                        if (Objects.equals(entry.getContestant().getId(), contestant.getId())) {
                            entries.add(Map.of("entry", entry, "round", r));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        model.addAttribute("title", contestant.getDisplayName());
        model.addAttribute("contestant", contestant);
        model.addAttribute("entries", entries);
        return "site/contestant";
    }

    @GetMapping("/results")
    public String results(Model model) {
        Season season = null;
        try {
            season = contestantService.getCurrentSeason();
        } catch (Exception ignored) {}

        List<Round> rounds = season != null ? new ArrayList<>(contestantService.listRounds(season.getId())) : new ArrayList<>();
        Collections.reverse(rounds);

        model.addAttribute("title", "Results");
        model.addAttribute("rounds", rounds);
        return "site/results";
    }

    @GetMapping("/results/{roundId}")
    public String leaderboard(@PathVariable("roundId") Long roundId, Model model) {
        VotingDtos.RoundTallyDto tally = votingService.getRoundTally(roundId);
        model.addAttribute("title", "Leaderboard - " + (tally.getRound() != null ? tally.getRound().getName() : ""));
        model.addAttribute("tally", tally);
        return "site/leaderboard";
    }

    @GetMapping("/bundles")
    public String bundles(Model model) {
        List<VoteBundle> bundles = paymentService.listBundles();
        model.addAttribute("title", "Vote Bundles");
        model.addAttribute("bundles", bundles);
        return "site/bundles";
    }

    @GetMapping("/partners")
    public String partners(Model model) {
        List<SponsorshipPackage> packages = sponsorService.listPackages();
        model.addAttribute("title", "Our Partners");
        model.addAttribute("packages", packages);
        return "site/partners";
    }

    @GetMapping("/sponsor-click/{agreementId}")
    public String sponsorClick(@PathVariable("agreementId") Long agreementId) {
        try {
            sponsorService.recordClick(agreementId);
            SponsorshipAgreement agreement = sponsorService.getAgreement(agreementId);
            String url = agreement.getClickThroughUrl();
            if (url == null || url.trim().isEmpty()) {
                url = agreement.getSponsor().getWebsite();
            }
            if (url != null && !url.trim().isEmpty()) {
                return "redirect:" + url;
            }
        } catch (Exception ignored) {}
        return "redirect:/partners";
    }
}
