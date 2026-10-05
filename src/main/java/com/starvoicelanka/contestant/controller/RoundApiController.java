package com.starvoicelanka.contestant.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.RoundEntry;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.service.ContestantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rounds")
public class RoundApiController {

    private final ContestantService contestantService;

    public RoundApiController(ContestantService contestantService) {
        this.contestantService = contestantService;
    }

    // Seasons
    @GetMapping("/seasons/current")
    public ResponseEntity<ApiResponse<Season>> currentSeason() {
        return ResponseEntity.ok(ApiResponse.ok(contestantService.getCurrentSeason()));
    }

    @PostMapping("/seasons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Season>> createSeason(@Valid @RequestBody ContestantDtos.CreateSeasonRequest req) {
        Season season = contestantService.createSeason(req.getName(), req.getYear(), req.getIsCurrent() != null && req.getIsCurrent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(season));
    }

    // Rounds
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContestantDtos.RoundDto>>> listRounds(@RequestParam(required = false) Long seasonId) {
        List<Round> rounds = contestantService.listRounds(seasonId);
        List<ContestantDtos.RoundDto> dtos = rounds.stream().map(ContestantDtos.RoundDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContestantDtos.RoundDto>> getRound(@PathVariable Long id) {
        Round round = contestantService.getRound(id);
        return ResponseEntity.ok(ApiResponse.ok(ContestantDtos.RoundDto.from(round)));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<ApiResponse<ContestantDtos.LeaderboardDto>> leaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contestantService.getLeaderboard(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.RoundDto>> createRound(@Valid @RequestBody ContestantDtos.CreateRoundRequest req) {
        Round round = contestantService.createRound(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ContestantDtos.RoundDto.from(round)));
    }

    @GetMapping("/{id}/line-up")
    public ResponseEntity<ApiResponse<List<ContestantDtos.RoundEntryDto>>> lineUp(@PathVariable Long id) {
        List<RoundEntry> entries = contestantService.getRoundLineUp(id);
        List<ContestantDtos.RoundEntryDto> dtos = entries.stream().map(ContestantDtos.RoundEntryDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/{id}/line-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ContestantDtos.RoundEntryDto>>> assignLineUp(
            @PathVariable Long id,
            @Valid @RequestBody ContestantDtos.AssignLineUpRequest req) {
        List<RoundEntry> entries = contestantService.assignContestantsToRound(id, req.getContestantIds());
        List<ContestantDtos.RoundEntryDto> dtos = entries.stream().map(ContestantDtos.RoundEntryDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/{id}/publish-results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.LeaderboardDto>> publishResults(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contestantService.publishResults(id)));
    }

    // CM06 - Performance media upload
    @PostMapping("/{id}/contestants/{contestantId}/media")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.RoundEntryDto>> uploadMedia(
            @PathVariable Long id,
            @PathVariable Long contestantId,
            @RequestParam("media") MultipartFile file,
            @RequestParam(value = "performanceTitle", required = false) String performanceTitle) {

        RoundEntry entry = contestantService.uploadPerformanceMedia(id, contestantId, file, performanceTitle);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ContestantDtos.RoundEntryDto.from(entry)));
    }

    @DeleteMapping("/{id}/contestants/{contestantId}/media")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.RoundEntryDto>> removeMedia(
            @PathVariable Long id,
            @PathVariable Long contestantId) {

        RoundEntry entry = contestantService.removePerformanceMedia(id, contestantId);
        return ResponseEntity.ok(ApiResponse.ok(ContestantDtos.RoundEntryDto.from(entry)));
    }
}
