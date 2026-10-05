package com.starvoicelanka.sponsor.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.PagedResponse;
import com.starvoicelanka.sponsor.dto.SponsorDtos;
import com.starvoicelanka.sponsor.entity.*;
import com.starvoicelanka.sponsor.service.SponsorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sponsors")
public class SponsorApiController {

    private final SponsorService sponsorService;

    public SponsorApiController(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    // SM02 - public rate card
    @GetMapping("/packages")
    public ApiResponse<List<SponsorshipPackage>> listPackages() {
        return ApiResponse.success(sponsorService.listPackages());
    }

    // SM02 - create package (admin)
    @PostMapping("/packages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SponsorshipPackage>> createPackage(@Valid @RequestBody SponsorDtos.CreatePackageRequest request) {
        SponsorshipPackage pkg = sponsorService.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(pkg));
    }

    // SM04 - banners served on vote page / leaderboard
    @GetMapping("/banners/{roundId}")
    public ApiResponse<List<SponsorDtos.BannerDto>> banners(
            @PathVariable("roundId") Long roundId,
            @RequestParam(name = "placement", required = false) ImpressionPlacement placement) {
        return ApiResponse.success(sponsorService.getBannersForRound(roundId, placement));
    }

    // SM04 - record banner click
    @PostMapping("/agreements/{id}/click")
    public ApiResponse<SponsorImpression> recordClick(@PathVariable("id") Long id) {
        return ApiResponse.success(sponsorService.recordClick(id));
    }

    // SM03 - list agreements
    @GetMapping("/agreements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<List<SponsorshipAgreement>> listAgreements(
            @RequestParam(name = "sponsorId", required = false) Long sponsorId,
            @RequestParam(name = "status", required = false) AgreementStatus status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SponsorshipAgreement> result = sponsorService.listAgreements(sponsorId, status, pageable);
        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(result.getContent(), meta);
    }

    // SM03 - create agreement
    @PostMapping("/agreements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SponsorshipAgreement>> createAgreement(@Valid @RequestBody SponsorDtos.CreateAgreementRequest request) {
        SponsorshipAgreement agreement = sponsorService.createAgreement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(agreement));
    }

    // SM03 - activate agreement
    @PostMapping("/agreements/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SponsorshipAgreement> activateAgreement(@PathVariable("id") Long id) {
        return ApiResponse.success(sponsorService.activateAgreement(id));
    }

    // SM03 / SM06 - terminate agreement
    @PostMapping("/agreements/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SponsorshipAgreement> terminateAgreement(
            @PathVariable("id") Long id,
            @Valid @RequestBody SponsorDtos.TerminateAgreementRequest request) {
        return ApiResponse.success(sponsorService.terminateAgreement(id, request.getReason()));
    }

    // SM05 - exposure report
    @GetMapping("/agreements/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<SponsorDtos.ExposureReportDto> exposureReport(@PathVariable("id") Long id) {
        return ApiResponse.success(sponsorService.getExposureReport(id));
    }

    // SM06 - renew agreement
    @PostMapping("/agreements/{id}/renew")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SponsorshipAgreement>> renewAgreement(
            @PathVariable("id") Long id,
            @RequestBody(required = false) SponsorDtos.RenewAgreementRequest request) {
        SponsorDtos.RenewAgreementRequest req = request != null ? request : new SponsorDtos.RenewAgreementRequest();
        SponsorshipAgreement renewal = sponsorService.renewAgreement(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(renewal));
    }

    // SM06 - trigger expire lapsed
    @PostMapping("/agreements/expire-lapsed")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SponsorDtos.LapsedSweepResult> expireLapsed() {
        return ApiResponse.success(sponsorService.expireLapsedAgreements(LocalDateTime.now()));
    }

    // SM06 - list invoices
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<List<SponsorInvoice>> listInvoices(
            @RequestParam(name = "sponsorId", required = false) Long sponsorId,
            @RequestParam(name = "agreementId", required = false) Long agreementId,
            @RequestParam(name = "status", required = false) InvoiceStatus status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SponsorInvoice> result = sponsorService.listInvoices(sponsorId, agreementId, status, pageable);
        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(result.getContent(), meta);
    }

    // SM06 - agreement financials
    @GetMapping("/agreements/{id}/financials")
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<SponsorDtos.FinancialsDto> financials(@PathVariable("id") Long id) {
        return ApiResponse.success(sponsorService.getAgreementFinancials(id));
    }

    // SM06 - issue invoice
    @PostMapping("/agreements/{id}/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SponsorInvoice>> issueInvoice(
            @PathVariable("id") Long id,
            @RequestBody(required = false) SponsorDtos.IssueInvoiceRequest request) {
        SponsorInvoice invoice = sponsorService.issueInvoice(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(invoice));
    }

    // SM06 - pay invoice
    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SponsorInvoice> payInvoice(
            @PathVariable("invoiceId") Long invoiceId,
            @Valid @RequestBody SponsorDtos.RecordInvoicePaymentRequest request) {
        return ApiResponse.success(sponsorService.recordInvoicePayment(invoiceId, request));
    }

    // SM06 - cancel invoice
    @PostMapping("/invoices/{invoiceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SponsorInvoice> cancelInvoice(
            @PathVariable("invoiceId") Long invoiceId,
            @Valid @RequestBody SponsorDtos.CancelInvoiceRequest request) {
        return ApiResponse.success(sponsorService.cancelInvoice(invoiceId, request.getReason()));
    }

    // SM01 - list sponsors
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<List<Sponsor>> listSponsors(
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "companyName"));

        Page<Sponsor> result = sponsorService.listSponsors(active, pageable);
        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(result.getContent(), meta);
    }

    // SM01 - get sponsor by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
    public ApiResponse<Sponsor> getSponsor(@PathVariable("id") Long id) {
        return ApiResponse.success(sponsorService.getSponsor(id));
    }

    // SM01 - register sponsor
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Sponsor>> registerSponsor(@Valid @RequestBody SponsorDtos.RegisterSponsorRequest request) {
        Sponsor sponsor = sponsorService.registerSponsor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sponsor));
    }

    // SM01 - update sponsor
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Sponsor> updateSponsor(
            @PathVariable("id") Long id,
            @RequestBody SponsorDtos.UpdateSponsorRequest request) {
        return ApiResponse.success(sponsorService.updateSponsor(id, request));
    }
}
