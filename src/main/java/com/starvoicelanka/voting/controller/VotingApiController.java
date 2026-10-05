package com.starvoicelanka.voting.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.PagedResponse;
import com.starvoicelanka.common.exception.UnauthorizedException;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.service.UserService;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;
import com.starvoicelanka.voting.service.VotingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
public class VotingApiController {

    private final VotingService votingService;
    private final UserService userService;

    public VotingApiController(VotingService votingService, UserService userService) {
        this.votingService = votingService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("You need to be logged in");
        }
        return userService.getUserByEmail(userDetails.getUsername());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // VM01 - Cast a Vote
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> castVote(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VotingDtos.CastVoteRequest request,
            HttpServletRequest servletRequest) {
        User user = getAuthenticatedUser(userDetails);
        if (!user.isMobileVerified()) {
            throw new UnauthorizedException("Please verify your mobile number before voting");
        }

        String ip = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        Map<String, Object> result = votingService.castVote(
                user.getId(),
                request.getRoundId(),
                request.getContestantId(),
                request.getCount(),
                ip,
                userAgent
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    // VM02 - Eligibility check
    @GetMapping("/eligibility/{roundId}")
    public ApiResponse<VotingDtos.EligibilityDto> eligibility(
            @PathVariable("roundId") Long roundId,
            @RequestParam(name = "count", defaultValue = "1") int count,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        VotingDtos.EligibilityDto dto = votingService.checkEligibility(user.getId(), roundId, count);
        return ApiResponse.success(dto);
    }

    // VM03 - Live tally
    @GetMapping("/tally/{roundId}")
    public ApiResponse<VotingDtos.RoundTallyDto> tally(@PathVariable("roundId") Long roundId) {
        return ApiResponse.success(votingService.getRoundTally(roundId));
    }

    // VM04 - Open round
    @PostMapping("/rounds/{roundId}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Round> openRound(@PathVariable("roundId") Long roundId) {
        return ApiResponse.success(votingService.openRound(roundId, true));
    }

    // VM04 - Close round
    @PostMapping("/rounds/{roundId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Round> closeRound(@PathVariable("roundId") Long roundId) {
        return ApiResponse.success(votingService.closeRound(roundId));
    }

    // VM05 - My votes
    @GetMapping("/me")
    public ApiResponse<List<VotingDtos.VoteDto>> myVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "roundId", required = false) Long roundId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        User user = getAuthenticatedUser(userDetails);
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Vote> result = votingService.listVotes(user.getId(), roundId, pageable);
        List<VotingDtos.VoteDto> dtos = result.getContent().stream().map(VotingDtos.VoteDto::from).toList();

        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(dtos, meta);
    }

    // VM05 - Round votes ledger (admin)
    @GetMapping("/rounds/{roundId}/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<VotingDtos.VoteDto>> roundVotes(
            @PathVariable("roundId") Long roundId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 200);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Vote> result = votingService.listVotes(null, roundId, pageable);
        List<VotingDtos.VoteDto> dtos = result.getContent().stream().map(VotingDtos.VoteDto::from).toList();

        PagedResponse.PaginationMeta meta = new PagedResponse.PaginationMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ApiResponse.success(dtos, meta);
    }

    // VM05 - Void vote (admin)
    @PostMapping("/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VotingDtos.VoteDto> voidVote(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        Vote vote = votingService.voidVote(id, reason);
        return ApiResponse.success(VotingDtos.VoteDto.from(vote));
    }

    // VM06 - Fraud detection & anomalies
    @PostMapping("/rounds/{roundId}/anomalies")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VotingDtos.AnomalyReportDto> anomalies(
            @PathVariable("roundId") Long roundId,
            @RequestParam(name = "autoVoid", defaultValue = "false") boolean autoVoid,
            @RequestParam(name = "notify", defaultValue = "true") boolean notify,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        VotingDtos.AnomalyReportDto report = votingService.detectAnomalies(roundId, autoVoid, notify, user.getId());
        return ApiResponse.success(report);
    }
}
