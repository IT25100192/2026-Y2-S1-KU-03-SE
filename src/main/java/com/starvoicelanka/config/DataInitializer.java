package com.starvoicelanka.config;

import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.ContestantStatus;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.repository.ContestantRepository;
import com.starvoicelanka.contestant.repository.RoundRepository;
import com.starvoicelanka.contestant.repository.SeasonRepository;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.entity.VoteBundle;
import com.starvoicelanka.payment.repository.CreditAccountRepository;
import com.starvoicelanka.payment.repository.VoteBundleRepository;
import com.starvoicelanka.sponsor.entity.AgreementStatus;
import com.starvoicelanka.sponsor.entity.Sponsor;
import com.starvoicelanka.sponsor.entity.SponsorshipAgreement;
import com.starvoicelanka.sponsor.entity.SponsorshipPackage;
import com.starvoicelanka.sponsor.entity.SponsorshipTier;
import com.starvoicelanka.sponsor.repository.SponsorRepository;
import com.starvoicelanka.sponsor.repository.SponsorshipAgreementRepository;
import com.starvoicelanka.sponsor.repository.SponsorshipPackageRepository;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import com.starvoicelanka.user.repository.UserRepository;
import com.starvoicelanka.voting.service.VotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;
    private final RoundRepository roundRepository;
    private final ContestantRepository contestantRepository;
    private final ContestantService contestantService;
    private final VoteBundleRepository bundleRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final SponsorshipPackageRepository packageRepository;
    private final SponsorRepository sponsorRepository;
    private final SponsorshipAgreementRepository agreementRepository;
    private final VotingService votingService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           SeasonRepository seasonRepository,
                           RoundRepository roundRepository,
                           ContestantRepository contestantRepository,
                           ContestantService contestantService,
                           VoteBundleRepository bundleRepository,
                           CreditAccountRepository creditAccountRepository,
                           SponsorshipPackageRepository packageRepository,
                           SponsorRepository sponsorRepository,
                           SponsorshipAgreementRepository agreementRepository,
                           VotingService votingService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.seasonRepository = seasonRepository;
        this.roundRepository = roundRepository;
        this.contestantRepository = contestantRepository;
        this.contestantService = contestantService;
        this.bundleRepository = bundleRepository;
        this.creditAccountRepository = creditAccountRepository;
        this.packageRepository = packageRepository;
        this.sponsorRepository = sponsorRepository;
        this.agreementRepository = agreementRepository;
        this.votingService = votingService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initialization.");
            return;
        }

        log.info("Starting database seeding (reproducing seed.js demo data)...");

        /* ---- Users ---- */
        User admin = new User("Show Control Admin", "admin@starvoice.lk", "0771234567", Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("admin12345"));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setVerifiedAt(LocalDateTime.now());
        userRepository.save(admin);

        List<User> voters = new ArrayList<>();
        String[] voterNames = {"Nimali Fernando", "Kasun Jayawardena", "Tharushi Perera", "Ravindu Silva", "Ishara Bandara"};
        for (int i = 0; i < voterNames.length; i++) {
            User voter = new User(
                    voterNames[i],
                    String.format("voter%d@starvoice.lk", i + 1),
                    String.format("07712345%02d", 70 + i),
                    Role.VOTER
            );
            voter.setPasswordHash(passwordEncoder.encode("voter12345"));
            voter.setStatus(UserStatus.ACTIVE);
            voter.setVerifiedAt(LocalDateTime.now());
            voters.add(userRepository.save(voter));
        }

        /* ---- Season, Contestants, Round ---- */
        Season season = new Season("StarVoice Lanka Season 3", 2026);
        season.setCurrent(true);
        season = seasonRepository.save(season);

        List<Contestant> contestants = new ArrayList<>();
        String[][] rawContestants = {
                {"Amaya Weeratunga", "Amaya", "22", "Kandy"},
                {"Dinuka Rajapaksha", "Dinuka R", "26", "Colombo"},
                {"Sanduni Alwis", "Sandu", "19", "Galle"},
                {"Malith Gunasekara", "Malith G", "24", "Kurunegala"},
                {"Hiruni Dissanayake", "Hiruni", "21", "Matara"}
        };

        for (String[] raw : rawContestants) {
            Contestant c = new Contestant(season, raw[0], raw[1]);
            c.setAge(Integer.parseInt(raw[2]));
            c.setDistrict(raw[3]);
            c.setBio(raw[1] + " from " + raw[3] + ".");
            c.setStatus(ContestantStatus.ACTIVE);
            contestants.add(contestantRepository.save(c));
        }

        Round round = new Round(season, "Quarter Final 1", 1);
        round.setClosesAt(LocalDateTime.now().plusDays(7));
        round.setAdvanceCount(3);
        round = roundRepository.save(round);

        List<Long> contestantIds = contestants.stream().map(Contestant::getId).toList();
        contestantService.assignContestantsToRound(round.getId(), contestantIds);

        /* ---- Payment: bundles and starting balances ---- */
        bundleRepository.save(new VoteBundle("STARTER", "Starter Pack", 10, 250.0, true));
        bundleRepository.save(new VoteBundle("FAN", "Fan Pack", 50, 1000.0, true));
        bundleRepository.save(new VoteBundle("SUPERFAN", "Superfan Pack", 200, 3500.0, true));

        for (User voter : voters) {
            CreditAccount account = new CreditAccount(voter);
            account.setBalance(20);
            account.setLifetimePurchased(20);
            creditAccountRepository.save(account);
        }

        /* ---- Sponsors ---- */
        SponsorshipPackage titlePkg = packageRepository.save(new SponsorshipPackage(
                SponsorshipTier.TITLE, "Title Partner", 4500000.0, 500000, 3, true, true
        ));
        packageRepository.save(new SponsorshipPackage(
                SponsorshipTier.GOLD, "Gold Partner", 1800000.0, 150000, 2, true, false
        ));
        packageRepository.save(new SponsorshipPackage(
                SponsorshipTier.SILVER, "Silver Partner", 750000.0, 50000, 1, false, false
        ));

        Sponsor sponsor = new Sponsor("Ceylon Telecom", "Priyantha Ratnayake", "partnerships@example.lk");
        sponsor.setIndustry("Telecommunications");
        sponsor.setWebsite("https://example.lk");
        sponsor.setContactPhone("0112345678");
        sponsor = sponsorRepository.save(sponsor);

        SponsorshipAgreement agreement = new SponsorshipAgreement();
        agreement.setAgreementNo("SVL-SP-2026-0001");
        agreement.setSponsor(sponsor);
        agreement.setPackageRef(titlePkg);
        agreement.setSeason(season);
        agreement.setContractValueLKR(titlePkg.getPriceLKR());
        agreement.setStartsAt(LocalDateTime.now().minusDays(1));
        agreement.setEndsAt(LocalDateTime.now().plusDays(90));
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setClickThroughUrl("https://example.lk/starvoice");
        agreementRepository.save(agreement);

        /* ---- Open the round and cast seed votes ---- */
        votingService.openRound(round.getId(), false);

        int[][] plan = {
                {0, 0, 5},
                {1, 0, 3},
                {2, 1, 7},
                {3, 2, 4},
                {4, 1, 6},
                {0, 3, 2}
        };

        for (int[] p : plan) {
            User voter = voters.get(p[0]);
            Contestant contestant = contestants.get(p[1]);
            votingService.castVote(voter.getId(), round.getId(), contestant.getId(), p[2], "127.0.0.1", "seed-script");
        }

        log.info("Database seeding completed successfully!");
        log.info("Log in as admin@starvoice.lk / admin12345 or voter1@starvoice.lk / voter12345");
    }
}
