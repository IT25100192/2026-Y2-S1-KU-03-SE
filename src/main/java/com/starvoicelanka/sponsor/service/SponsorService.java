package com.starvoicelanka.sponsor.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.common.exception.ResourceNotFoundException;
import com.starvoicelanka.common.validation.InputValidator;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.repository.RoundRepository;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.sponsor.dto.SponsorDtos;
import com.starvoicelanka.sponsor.entity.*;
import com.starvoicelanka.sponsor.repository.*;
import com.starvoicelanka.sponsor.service.pricing.BasePrice;
import com.starvoicelanka.sponsor.service.pricing.ContractPrice;
import com.starvoicelanka.sponsor.service.pricing.UpliftDecorator;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SponsorService {

    private static final Logger log = LoggerFactory.getLogger(SponsorService.class);

    private final SponsorRepository sponsorRepository;
    private final SponsorshipPackageRepository packageRepository;
    private final SponsorshipAgreementRepository agreementRepository;
    private final SponsorImpressionRepository impressionRepository;
    private final SponsorInvoiceRepository invoiceRepository;
    private final RoundRepository roundRepository;
    private final ContestantService contestantService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    public SponsorService(SponsorRepository sponsorRepository,
                          SponsorshipPackageRepository packageRepository,
                          SponsorshipAgreementRepository agreementRepository,
                          SponsorImpressionRepository impressionRepository,
                          SponsorInvoiceRepository invoiceRepository,
                          RoundRepository roundRepository,
                          ContestantService contestantService,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.sponsorRepository = sponsorRepository;
        this.packageRepository = packageRepository;
        this.agreementRepository = agreementRepository;
        this.impressionRepository = impressionRepository;
        this.invoiceRepository = invoiceRepository;
        this.roundRepository = roundRepository;
        this.contestantService = contestantService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /* ------------------------------------------------------------------ */
    /* SM01 - Register and manage sponsors                                 */
    /* ------------------------------------------------------------------ */

    public Sponsor registerSponsor(SponsorDtos.RegisterSponsorRequest req) {
        String companyName = InputValidator.requireText(req.getCompanyName(), "Company name", 2, 150);
        String contactName = InputValidator.requireText(req.getContactName(), "Contact name", 2, 100);
        String contactEmail = InputValidator.requireEmail(req.getContactEmail(), "Contact email", 150);
        String industry = InputValidator.optionalText(req.getIndustry(), "Industry", 100);
        String website = InputValidator.optionalUrl(req.getWebsite(), "Website", 255);
        String logoUrl = InputValidator.optionalUrl(req.getLogoUrl(), "Logo link", 500);
        String contactPhone = InputValidator.optionalPhone(req.getContactPhone(), "Contact phone");

        if (sponsorRepository.existsByCompanyNameIgnoreCase(companyName)) {
            throw new ConflictException("That company is already registered as a sponsor");
        }
        Sponsor sponsor = new Sponsor(companyName, contactName, contactEmail);
        sponsor.setIndustry(industry);
        sponsor.setWebsite(website);
        sponsor.setLogoUrl(logoUrl);
        sponsor.setContactPhone(contactPhone);

        if (req.getAccountManagerId() != null) {
            User am = userRepository.findById(req.getAccountManagerId()).orElse(null);
            sponsor.setAccountManager(am);
        }

        return sponsorRepository.save(sponsor);
    }

    public Sponsor updateSponsor(Long sponsorId, SponsorDtos.UpdateSponsorRequest changes) {
        Sponsor sponsor = getSponsor(sponsorId);
        if (changes.getCompanyName() != null && !changes.getCompanyName().trim().isEmpty()) {
            InputValidator.requireText(changes.getCompanyName(), "Company name", 2, 150);
            if (!sponsor.getCompanyName().equalsIgnoreCase(changes.getCompanyName().trim()) &&
                    sponsorRepository.existsByCompanyNameIgnoreCase(changes.getCompanyName().trim())) {
                throw new ConflictException("That company is already registered as a sponsor");
            }
            sponsor.setCompanyName(changes.getCompanyName().trim());
        }
        if (changes.getIndustry() != null) sponsor.setIndustry(InputValidator.optionalText(changes.getIndustry(), "Industry", 100));
        if (changes.getWebsite() != null) sponsor.setWebsite(InputValidator.optionalUrl(changes.getWebsite(), "Website", 255));
        if (changes.getLogoUrl() != null) sponsor.setLogoUrl(InputValidator.optionalUrl(changes.getLogoUrl(), "Logo link", 500));
        if (changes.getContactName() != null) sponsor.setContactName(InputValidator.requireText(changes.getContactName(), "Contact name", 2, 100));
        if (changes.getContactEmail() != null) sponsor.setContactEmail(InputValidator.requireEmail(changes.getContactEmail(), "Contact email", 150));
        if (changes.getContactPhone() != null) sponsor.setContactPhone(InputValidator.optionalPhone(changes.getContactPhone(), "Contact phone"));
        if (changes.getIsActive() != null) sponsor.setActive(changes.getIsActive());

        if (changes.getAccountManagerId() != null) {
            User am = userRepository.findById(changes.getAccountManagerId()).orElse(null);
            sponsor.setAccountManager(am);
        }

        return sponsorRepository.save(sponsor);
    }

    @Transactional(readOnly = true)
    public Page<Sponsor> listSponsors(Boolean isActive, Pageable pageable) {
        Specification<Sponsor> spec = (root, query, cb) -> {
            if (isActive == null) return cb.conjunction();
            return cb.equal(root.get("isActive"), isActive);
        };
        return sponsorRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Sponsor getSponsor(Long sponsorId) {
        return sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor not found: " + sponsorId));
    }

    public void deleteAgreement(Long agreementId) {
        SponsorshipAgreement agreement = getAgreement(agreementId);
        agreementRepository.nullifyRenewedFrom(agreementId);
        impressionRepository.deleteByAgreementId(agreementId);
        invoiceRepository.deleteByAgreementId(agreementId);
        agreementRepository.delete(agreement);
    }

    public void deleteSponsor(Long sponsorId) {
        Sponsor sponsor = getSponsor(sponsorId);
        List<SponsorshipAgreement> agreements = agreementRepository.findBySponsorId(sponsorId);
        for (SponsorshipAgreement ag : agreements) {
            deleteAgreement(ag.getId());
        }
        sponsorRepository.delete(sponsor);
    }

    /* ------------------------------------------------------------------ */
    /* SM02 - Sponsorship packages                                         */
    /* ------------------------------------------------------------------ */

    public SponsorshipPackage createPackage(SponsorDtos.CreatePackageRequest req) {
        if (req.getTier() == null) {
            throw new com.starvoicelanka.common.exception.ValidationException("Choose a sponsorship tier");
        }
        String pkgName = InputValidator.requireText(req.getName(), "Package name", 2, 100);
        double pkgPrice = InputValidator.requireMoney(req.getPriceLKR(), "Package price", false, 1_000_000_000d);
        InputValidator.requireRange(req.getGuaranteedImpressions(), "Guaranteed impressions", 0, 100_000_000);
        InputValidator.requireRange(req.getBannerSlotsPerRound(), "Banner slots per round", 0, 20);
        Optional<SponsorshipPackage> existing = packageRepository.findByTier(req.getTier());
        if (existing.isPresent()) {
            throw new ConflictException("A package for tier " + req.getTier() + " already exists");
        }
        SponsorshipPackage pkg = new SponsorshipPackage(
                req.getTier(),
                pkgName,
                pkgPrice,
                req.getGuaranteedImpressions(),
                req.getBannerSlotsPerRound(),
                req.isLogoOnLeaderboard(),
                req.isNamingRights()
        );
        return packageRepository.save(pkg);
    }

    /** UPDATE: edit a package's name, price and benefits (the tier itself stays fixed). */
    public SponsorshipPackage updatePackage(Long packageId, String name, Double priceLKR, Integer guaranteedImpressions,
                                            Integer bannerSlotsPerRound, Boolean logoOnLeaderboard, Boolean namingRights) {
        SponsorshipPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageId));
        if (name != null && !name.isBlank()) pkg.setName(InputValidator.requireText(name, "Package name", 2, 100));
        if (priceLKR != null) {
            pkg.setPriceLKR(InputValidator.requireMoney(priceLKR, "Package price", false, 1_000_000_000d));
        }
        if (guaranteedImpressions != null) pkg.setGuaranteedImpressions(InputValidator.requireRange(guaranteedImpressions, "Guaranteed impressions", 0, 100_000_000));
        if (bannerSlotsPerRound != null) pkg.setBannerSlotsPerRound(InputValidator.requireRange(bannerSlotsPerRound, "Banner slots per round", 0, 20));
        if (logoOnLeaderboard != null) pkg.setLogoOnLeaderboard(logoOnLeaderboard);
        if (namingRights != null) pkg.setNamingRights(namingRights);
        return packageRepository.save(pkg);
    }

    /** DELETE: remove a package that no agreement uses. */
    public void deletePackage(Long packageId) {
        SponsorshipPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + packageId));
        if (agreementRepository.existsByPackageRefId(packageId)) {
            throw new BadRequestException("This package is used by an existing agreement, so it cannot be deleted");
        }
        packageRepository.delete(pkg);
    }

    @Transactional(readOnly = true)
    public List<SponsorshipPackage> listPackages() {
        return packageRepository.findAllByOrderByPriceLKRDesc();
    }

    /* ------------------------------------------------------------------ */
    /* SM03 - Agreements                                                   */
    /* ------------------------------------------------------------------ */

    private String nextAgreementNo() {
        int year = LocalDateTime.now().getYear();
        int serial = 1000 + random.nextInt(9000);
        return String.format("SVL-SP-%d-%04d", year, serial);
    }

    public SponsorshipAgreement createAgreement(SponsorDtos.CreateAgreementRequest req) {
        InputValidator.requireId(req.getSponsorId(), "sponsor");
        if (req.getPackageTier() == null) {
            throw new com.starvoicelanka.common.exception.ValidationException("Choose a sponsorship package");
        }
        if (req.getContractValueLKR() != null) {
            InputValidator.requireMoney(req.getContractValueLKR(), "Contract value", false, 1_000_000_000d);
        }
        InputValidator.optionalUrl(req.getBannerImageUrl(), "Banner image link", 500);
        InputValidator.optionalUrl(req.getClickThroughUrl(), "Click-through link", 500);
        if (req.getStartsAt() == null || req.getEndsAt() == null) {
            throw new com.starvoicelanka.common.exception.ValidationException("Start and end dates are required");
        }
        Sponsor sponsor = getSponsor(req.getSponsorId());
        SponsorshipPackage pkg = packageRepository.findByTier(req.getPackageTier())
                .orElseThrow(() -> new ResourceNotFoundException("No sponsorship package for tier " + req.getPackageTier()));

        Season season;
        if (req.getSeasonId() != null) {
            season = contestantService.getSeason(req.getSeasonId());
        } else {
            season = contestantService.getCurrentSeason();
        }

        if (!req.getEndsAt().isAfter(req.getStartsAt())) {
            throw new BadRequestException("The agreement end date must be after the start date");
        }

        Optional<SponsorshipAgreement> clash = agreementRepository.findClash(
                sponsor.getId(), season.getId(), List.of(AgreementStatus.DRAFT, AgreementStatus.ACTIVE));
        if (clash.isPresent()) {
            throw new ConflictException(sponsor.getCompanyName() + " already has agreement " + clash.get().getAgreementNo() + " for this season");
        }

        SponsorshipAgreement agreement = new SponsorshipAgreement();
        agreement.setAgreementNo(nextAgreementNo());
        agreement.setSponsor(sponsor);
        agreement.setPackageRef(pkg);
        agreement.setSeason(season);
        agreement.setContractValueLKR(req.getContractValueLKR() != null ? req.getContractValueLKR() : pkg.getPriceLKR());
        agreement.setStartsAt(req.getStartsAt());
        agreement.setEndsAt(req.getEndsAt());
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setBannerImageUrl(req.getBannerImageUrl());
        agreement.setClickThroughUrl(req.getClickThroughUrl());

        return agreementRepository.save(agreement);
    }

    public SponsorshipAgreement activateAgreement(Long agreementId) {
        SponsorshipAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));

        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new BadRequestException("Only a draft agreement can be activated, this one is " + agreement.getStatus());
        }

        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement = agreementRepository.save(agreement);

        Long notifyUser = agreement.getSponsor().getAccountManager() != null ? agreement.getSponsor().getAccountManager().getId() : null;
        notificationService.dispatch(notifyUser, NotificationTemplate.SPONSOR_AGREEMENT, Map.of(
                "agreementNo", agreement.getAgreementNo(),
                "sponsorName", agreement.getSponsor().getCompanyName(),
                "tier", agreement.getPackageRef().getTier().name(),
                "seasonName", agreement.getSeason() != null ? agreement.getSeason().getName() : "the current season",
                "value", agreement.getContractValueLKR()
        ));

        return agreement;
    }

    /** UPDATE: change the package tier and contract value of a draft agreement. */
    public SponsorshipAgreement updateAgreement(Long agreementId, SponsorshipTier tier, Double contractValueLKR) {
        SponsorshipAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));
        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new BadRequestException("Only a draft agreement can be edited");
        }
        if (tier == null) {
            throw new com.starvoicelanka.common.exception.ValidationException("Choose a sponsorship package");
        }
        if (contractValueLKR != null) {
            InputValidator.requireMoney(contractValueLKR, "Contract value", false, 1_000_000_000d);
        }
        SponsorshipPackage pkg = packageRepository.findByTier(tier)
                .orElseThrow(() -> new ResourceNotFoundException("No sponsorship package for tier " + tier));
        agreement.setPackageRef(pkg);
        agreement.setContractValueLKR(contractValueLKR != null ? contractValueLKR : pkg.getPriceLKR());
        return agreementRepository.save(agreement);
    }

    public SponsorshipAgreement terminateAgreement(Long agreementId, String reason) {
        SponsorshipAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));

        if (agreement.getStatus() == AgreementStatus.TERMINATED) {
            throw new BadRequestException("That agreement is already terminated");
        }
        reason = InputValidator.optionalText(reason, "Reason", 500);

        agreement.setStatus(AgreementStatus.TERMINATED);
        agreement.setTerminatedAt(LocalDateTime.now());
        agreement.setTerminationReason(reason);
        return agreementRepository.save(agreement);
    }

    @Transactional(readOnly = true)
    public Page<SponsorshipAgreement> listAgreements(Long sponsorId, AgreementStatus status, Pageable pageable) {
        Specification<SponsorshipAgreement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sponsorId != null) {
                predicates.add(cb.equal(root.get("sponsor").get("id"), sponsorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return agreementRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public SponsorshipAgreement getAgreement(Long agreementId) {
        return agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));
    }

    /* ------------------------------------------------------------------ */
    /* SM04 - Banner delivery and impression tracking                      */
    /* ------------------------------------------------------------------ */

    public List<SponsorDtos.BannerDto> getBannersForRound(Long roundId, ImpressionPlacement placement) {
        try {
            return loadBannersForRound(roundId, placement != null ? placement : ImpressionPlacement.LEADERBOARD);
        } catch (Exception e) {
            // Voting Management calls this from inside the live tally. A sponsor
            // problem must not take the leaderboard down with it, so this fails soft.
            log.error("[sponsor] banner delivery failed, serving none: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SponsorDtos.BannerDto> loadBannersForRound(Long roundId, ImpressionPlacement placement) {
        LocalDateTime now = LocalDateTime.now();
        List<SponsorshipAgreement> agreements = agreementRepository.findActiveAgreementsInWindow(AgreementStatus.ACTIVE, now);

        Round round = null;
        if (roundId != null) {
            round = roundRepository.findById(roundId).orElse(null);
        }

        List<SponsorDtos.BannerDto> banners = new ArrayList<>();
        for (SponsorshipAgreement agreement : agreements) {
            int slots = agreement.getPackageRef().getBannerSlotsPerRound();
            String clickUrl = agreement.getClickThroughUrl() != null && !agreement.getClickThroughUrl().isEmpty()
                    ? agreement.getClickThroughUrl()
                    : agreement.getSponsor().getWebsite();

            banners.add(new SponsorDtos.BannerDto(
                    agreement.getId(),
                    agreement.getPackageRef().getTier(),
                    agreement.getSponsor().getCompanyName(),
                    agreement.getSponsor().getLogoUrl(),
                    agreement.getBannerImageUrl(),
                    clickUrl,
                    slots
            ));

            SponsorImpression imp = new SponsorImpression(agreement, round, placement);
            impressionRepository.save(imp);
        }

        // Tier sort: TITLE (0), GOLD (1), SILVER (2), BRONZE (3)
        banners.sort(Comparator.comparingInt(b -> b.tier().ordinal()));
        return banners;
    }

    public SponsorImpression recordClick(Long agreementId) {
        SponsorImpression impression = impressionRepository.findTopByAgreementIdOrderByCreatedAtDesc(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("No impression to attach that click to"));

        impression.setClicked(true);
        return impressionRepository.save(impression);
    }

    /* ------------------------------------------------------------------ */
    /* SM05 - Exposure report                                              */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public SponsorDtos.ExposureReportDto getExposureReport(Long agreementId) {
        SponsorshipAgreement agreement = getAgreement(agreementId);
        List<SponsorImpression> impressions = impressionRepository.findByAgreementIdWithRound(agreementId);

        Map<String, Map<String, Long>> byRound = new HashMap<>();
        long clicks = 0;
        for (SponsorImpression imp : impressions) {
            String key = imp.getRound() != null ? imp.getRound().getName() : "Unassigned";
            Map<String, Long> stats = byRound.computeIfAbsent(key, k -> new HashMap<>(Map.of("impressions", 0L, "clicks", 0L)));
            stats.put("impressions", stats.get("impressions") + 1);
            if (imp.isClicked()) {
                stats.put("clicks", stats.get("clicks") + 1);
                clicks++;
            }
        }

        long delivered = impressions.size();
        int guaranteed = agreement.getPackageRef() != null ? agreement.getPackageRef().getGuaranteedImpressions() : 0;
        Double fulfilment = guaranteed > 0 ? Math.round(((double) delivered / guaranteed) * 1000.0) / 10.0 : null;
        double ctr = delivered > 0 ? Math.round(((double) clicks / delivered) * 10000.0) / 100.0 : 0.0;
        Double cpi = delivered > 0 ? Math.round((agreement.getContractValueLKR() / delivered) * 100.0) / 100.0 : null;

        return new SponsorDtos.ExposureReportDto(
                agreement.getAgreementNo(),
                agreement.getSponsor() != null ? agreement.getSponsor().getCompanyName() : "Sponsor",
                agreement.getPackageRef() != null ? agreement.getPackageRef().getTier() : null,
                agreement.getContractValueLKR(),
                agreement.getStartsAt(),
                agreement.getEndsAt(),
                delivered,
                guaranteed,
                fulfilment,
                clicks,
                ctr,
                cpi,
                byRound
        );
    }

    /* ------------------------------------------------------------------ */
    /* SM06 - Renewal                                                      */
    /* ------------------------------------------------------------------ */

    public SponsorshipAgreement renewAgreement(Long agreementId, SponsorDtos.RenewAgreementRequest req) {
        SponsorshipAgreement previous = getAgreement(agreementId);

        if (previous.getStatus() == AgreementStatus.TERMINATED) {
            throw new BadRequestException("A terminated agreement cannot be renewed - raise a new one instead");
        }
        if (previous.getStatus() == AgreementStatus.DRAFT) {
            throw new BadRequestException("That agreement has not run yet, so there is nothing to renew");
        }

        Optional<SponsorshipAgreement> alreadyRenewed = agreementRepository.findByRenewedFromId(previous.getId());
        if (alreadyRenewed.isPresent()) {
            throw new ConflictException("That agreement was already renewed as " + alreadyRenewed.get().getAgreementNo());
        }

        double uplift = req.getUpliftPercent() != null ? req.getUpliftPercent() : 0.0;
        if (uplift < -100.0 || uplift > 500.0) {
            throw new BadRequestException("The uplift has to be between -100% and 500%");
        }

        Season season;
        if (req.getSeasonId() != null) {
            season = contestantService.getSeason(req.getSeasonId());
        } else {
            season = contestantService.getCurrentSeason();
        }

        if (Objects.equals(season.getId(), previous.getSeason().getId())) {
            throw new BadRequestException("That is the same season the agreement already covers - pick the next one");
        }

        // DECORATOR PATTERN: start from the previous contract value and wrap it with an uplift when asked
        ContractPrice price = new BasePrice("Previous contract " + previous.getAgreementNo(), previous.getContractValueLKR());
        if (uplift != 0.0) {
            price = new UpliftDecorator(price, uplift);
        }
        if (req.getContractValueLKR() != null) {
            InputValidator.requireMoney(req.getContractValueLKR(), "Contract value", false, 1_000_000_000d);
        }
        double value = req.getContractValueLKR() != null ? req.getContractValueLKR() : price.getAmount();

        LocalDateTime start = req.getStartsAt() != null ? req.getStartsAt() : previous.getEndsAt();
        Duration duration = Duration.between(previous.getStartsAt(), previous.getEndsAt());
        LocalDateTime end = req.getEndsAt() != null ? req.getEndsAt() : start.plus(duration);

        if (!end.isAfter(start)) {
            throw new BadRequestException("The renewal end date must be after its start date");
        }

        SponsorshipAgreement renewal = new SponsorshipAgreement();
        renewal.setAgreementNo(nextAgreementNo());
        renewal.setSponsor(previous.getSponsor());
        renewal.setPackageRef(previous.getPackageRef());
        renewal.setSeason(season);
        renewal.setContractValueLKR(value);
        renewal.setStartsAt(start);
        renewal.setEndsAt(end);
        renewal.setBannerImageUrl(previous.getBannerImageUrl());
        renewal.setClickThroughUrl(previous.getClickThroughUrl());
        renewal.setRenewedFrom(previous);
        renewal.setRenewedAt(LocalDateTime.now());
        renewal.setUpliftPercent(uplift);
        renewal.setStatus(AgreementStatus.DRAFT);
        renewal = agreementRepository.save(renewal);

        previous.setStatus(AgreementStatus.EXPIRED);
        agreementRepository.save(previous);

        Long notifyUser = previous.getSponsor().getAccountManager() != null ? previous.getSponsor().getAccountManager().getId() : null;
        notificationService.dispatch(notifyUser, NotificationTemplate.AGREEMENT_RENEWED, Map.of(
                "newAgreementNo", renewal.getAgreementNo(),
                "previousAgreementNo", previous.getAgreementNo(),
                "sponsorName", previous.getSponsor().getCompanyName(),
                "tier", previous.getPackageRef().getTier().name(),
                "value", value,
                "upliftPercent", uplift
        ));

        if (req.isActivate()) {
            return activateAgreement(renewal.getId());
        }
        return renewal;
    }

    public SponsorDtos.LapsedSweepResult expireLapsedAgreements(LocalDateTime now) {
        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();
        List<SponsorshipAgreement> lapsed = agreementRepository.findByStatusAndEndsAtBefore(AgreementStatus.ACTIVE, checkTime);

        List<String> agreementNos = new ArrayList<>();
        for (SponsorshipAgreement a : lapsed) {
            a.setStatus(AgreementStatus.EXPIRED);
            agreementRepository.save(a);
            agreementNos.add(a.getAgreementNo());
        }

        List<SponsorInvoice> overdueInvoices = invoiceRepository.findByStatusAndDueAtBefore(InvoiceStatus.ISSUED, checkTime);
        for (SponsorInvoice i : overdueInvoices) {
            i.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(i);
        }

        return new SponsorDtos.LapsedSweepResult(lapsed.size(), agreementNos, overdueInvoices.size());
    }

    /* ------------------------------------------------------------------ */
    /* SM06 - Invoicing                                                    */
    /* ------------------------------------------------------------------ */

    private String nextInvoiceNo() {
        int year = LocalDateTime.now().getYear();
        int serial = 1000 + random.nextInt(9000);
        return String.format("SVL-INV-%d-%04d", year, serial);
    }

    public SponsorInvoice issueInvoice(Long agreementId, SponsorDtos.IssueInvoiceRequest req) {
        SponsorshipAgreement agreement = getAgreement(agreementId);
        if (agreement.getStatus() == AgreementStatus.DRAFT) {
            throw new BadRequestException("Activate the agreement before invoicing against it");
        }
        if (req != null) {
            if (req.getAmountLKR() != null) {
                InputValidator.requireMoney(req.getAmountLKR(), "Invoice amount", false, 1_000_000_000d);
            }
            InputValidator.optionalText(req.getDescription(), "Invoice description", 500);
        }

        List<SponsorInvoice> existing = invoiceRepository.findByAgreementIdAndStatusNot(agreement.getId(), InvoiceStatus.CANCELLED);
        double raised = existing.stream().mapToDouble(SponsorInvoice::getAmountLKR).sum();

        double amount = (req != null && req.getAmountLKR() != null) ? req.getAmountLKR() : (agreement.getContractValueLKR() - raised);

        if (amount <= 0) {
            throw new BadRequestException("An invoice has to be for more than zero");
        }
        if (raised + amount > agreement.getContractValueLKR() + 0.01) {
            throw new BadRequestException(String.format(
                    "That would invoice LKR %.2f against a contract worth LKR %.2f. LKR %.2f is left to bill.",
                    raised + amount, agreement.getContractValueLKR(), agreement.getContractValueLKR() - raised));
        }

        LocalDateTime dueAt = (req != null && req.getDueAt() != null) ? req.getDueAt() : LocalDateTime.now().plusDays(30);
        String desc = (req != null && req.getDescription() != null) ? req.getDescription() : "Sponsorship fee - agreement " + agreement.getAgreementNo();

        SponsorInvoice invoice = new SponsorInvoice();
        invoice.setInvoiceNo(nextInvoiceNo());
        invoice.setAgreement(agreement);
        invoice.setSponsor(agreement.getSponsor());
        invoice.setAmountLKR(amount);
        invoice.setDescription(desc);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDueAt(dueAt);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice = invoiceRepository.save(invoice);

        Long notifyUser = agreement.getSponsor().getAccountManager() != null ? agreement.getSponsor().getAccountManager().getId() : null;
        notificationService.dispatch(notifyUser, NotificationTemplate.SPONSOR_INVOICE, Map.of(
                "invoiceNo", invoice.getInvoiceNo(),
                "sponsorName", agreement.getSponsor().getCompanyName(),
                "agreementNo", agreement.getAgreementNo(),
                "amount", invoice.getAmountLKR(),
                "dueAt", invoice.getDueAt().toLocalDate().toString()
        ));

        return invoice;
    }

    public SponsorInvoice recordInvoicePayment(Long invoiceId, SponsorDtos.RecordInvoicePaymentRequest req) {
        SponsorInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("That invoice is already settled");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("That invoice was cancelled");
        }
        if (req.getPaymentReference() == null || req.getPaymentReference().trim().isEmpty()) {
            throw new BadRequestException("Record the bank or cheque reference against the payment");
        }
        InputValidator.requireText(req.getPaymentReference(), "Payment reference", 3, 100);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(req.getPaidAt() != null ? req.getPaidAt() : LocalDateTime.now());
        invoice.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "BANK_TRANSFER");
        invoice.setPaymentReference(req.getPaymentReference().trim());
        return invoiceRepository.save(invoice);
    }

    public SponsorInvoice cancelInvoice(Long invoiceId, String reason) {
        SponsorInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("A settled invoice cannot be cancelled - raise a credit note instead");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledReason(InputValidator.optionalText(reason, "Reason", 500));
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Page<SponsorInvoice> listInvoices(Long sponsorId, Long agreementId, InvoiceStatus status, Pageable pageable) {
        Specification<SponsorInvoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sponsorId != null) {
                predicates.add(cb.equal(root.get("sponsor").get("id"), sponsorId));
            }
            if (agreementId != null) {
                predicates.add(cb.equal(root.get("agreement").get("id"), agreementId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return invoiceRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public SponsorDtos.FinancialsDto getAgreementFinancials(Long agreementId) {
        SponsorshipAgreement agreement = getAgreement(agreementId);
        List<SponsorInvoice> invoices = invoiceRepository.findByAgreementIdOrderByIssuedAtAsc(agreement.getId());
        List<SponsorInvoice> live = invoices.stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED).toList();

        double invoiced = live.stream().mapToDouble(SponsorInvoice::getAmountLKR).sum();
        double collected = live.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).mapToDouble(SponsorInvoice::getAmountLKR).sum();
        double overdue = live.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).mapToDouble(SponsorInvoice::getAmountLKR).sum();

        return new SponsorDtos.FinancialsDto(
                agreement.getAgreementNo(),
                agreement.getSponsor() != null ? agreement.getSponsor().getCompanyName() : "Sponsor",
                agreement.getPackageRef() != null ? agreement.getPackageRef().getTier() : null,
                agreement.getContractValueLKR(),
                invoiced,
                collected,
                invoiced - collected,
                overdue,
                agreement.getContractValueLKR() - invoiced,
                invoices
        );
    }
}
