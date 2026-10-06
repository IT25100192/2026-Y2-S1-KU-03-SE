package com.starvoicelanka.contestant.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.PagedResponse;
import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.ContestantStatus;
import com.starvoicelanka.contestant.service.ContestantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contestants")
public class ContestantApiController {

    private final ContestantService contestantService;

    public ContestantApiController(ContestantService contestantService) {
        this.contestantService = contestantService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ContestantDtos.ContestantDto>> listContestants(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) ContestantStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, limit), 100);
        Page<Contestant> cPage = contestantService.listContestants(
                seasonId, status, PageRequest.of(pageIndex, pageSize, Sort.by("totalVotes").descending().and(Sort.by("fullName").ascending())));

        List<ContestantDtos.ContestantDto> dtos = cPage.getContent().stream().map(ContestantDtos.ContestantDto::from).toList();
        return ResponseEntity.ok(new PagedResponse<>(dtos, page, pageSize, cPage.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContestantDtos.ContestantDto>> getContestant(@PathVariable Long id) {
        Contestant c = contestantService.getContestant(id);
        return ResponseEntity.ok(ApiResponse.ok(ContestantDtos.ContestantDto.from(c)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.ContestantDto>> registerContestant(
            @Valid @RequestBody ContestantDtos.RegisterContestantRequest req) {
        Contestant c = contestantService.registerContestant(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ContestantDtos.ContestantDto.from(c)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.ContestantDto>> updateContestant(
            @PathVariable Long id,
            @RequestBody ContestantDtos.UpdateContestantRequest req) {
        Contestant c = contestantService.updateContestant(id, req);
        return ResponseEntity.ok(ApiResponse.ok(ContestantDtos.ContestantDto.from(c)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContestantDtos.ContestantDto>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody ContestantDtos.SetContestantStatusRequest req) {
        Contestant c = contestantService.setContestantStatus(id, req.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(ContestantDtos.ContestantDto.from(c)));
    }
}
