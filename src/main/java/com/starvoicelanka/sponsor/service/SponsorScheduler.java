package com.starvoicelanka.sponsor.service;

import com.starvoicelanka.sponsor.dto.SponsorDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SponsorScheduler {

    private static final Logger log = LoggerFactory.getLogger(SponsorScheduler.class);

    private final SponsorService sponsorService;

    public SponsorScheduler(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    /**
     * SM06 - 00:15 every night, after the day has definitely rolled over.
     */
    @Scheduled(cron = "0 15 0 * * *")
    public void runSponsorExpiry() {
        try {
            SponsorDtos.LapsedSweepResult result = sponsorService.expireLapsedAgreements(LocalDateTime.now());
            if (result.expired() > 0 || result.invoicesMarkedOverdue() > 0) {
                log.info("[worker:sponsor-expiry] expired {} agreement(s), {} invoice(s) now overdue",
                        result.expired(), result.invoicesMarkedOverdue());
            }
        } catch (Exception e) {
            log.error("[worker:sponsor-expiry] failed: {}", e.getMessage(), e);
        }
    }
}
