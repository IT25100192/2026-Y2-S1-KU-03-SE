package com.starvoicelanka.sponsor;

import com.starvoicelanka.sponsor.dto.SponsorDtos;
import com.starvoicelanka.sponsor.entity.AgreementStatus;
import com.starvoicelanka.sponsor.entity.Sponsor;
import com.starvoicelanka.sponsor.entity.SponsorshipAgreement;
import com.starvoicelanka.sponsor.entity.SponsorshipTier;
import com.starvoicelanka.sponsor.repository.SponsorshipAgreementRepository;
import com.starvoicelanka.sponsor.service.SponsorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class SponsorModuleTests {

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private SponsorshipAgreementRepository agreementRepository;

    @Test
    void testRegisterSponsor() {
        SponsorDtos.RegisterSponsorRequest req = new SponsorDtos.RegisterSponsorRequest();
        req.setCompanyName("Dialog Axiata");
        req.setContactName("Sampath Perera");
        req.setContactEmail("sampath@dialog.lk");

        Sponsor sponsor = sponsorService.registerSponsor(req);
        assertNotNull(sponsor.getId());
        assertEquals("Dialog Axiata", sponsor.getCompanyName());
    }

    @Test
    void testGetBannersForRoundSoftFails() {
        // Must succeed without throwing even if roundId is invalid
        List<SponsorDtos.BannerDto> banners = sponsorService.getBannersForRound(999999L, null);
        assertNotNull(banners);
    }

    @Test
    void testExposureReportAndFinancials() {
        SponsorshipAgreement agreement = agreementRepository.findAll().get(0);
        SponsorDtos.ExposureReportDto report = sponsorService.getExposureReport(agreement.getId());
        assertNotNull(report);
        assertEquals(agreement.getAgreementNo(), report.agreementNo());

        SponsorDtos.FinancialsDto financials = sponsorService.getAgreementFinancials(agreement.getId());
        assertNotNull(financials);
        assertEquals(agreement.getContractValueLKR(), financials.contractValueLKR());
    }
}
