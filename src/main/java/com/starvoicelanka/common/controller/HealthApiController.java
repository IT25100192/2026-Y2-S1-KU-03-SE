package com.starvoicelanka.common.controller;

import com.starvoicelanka.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthApiController {

    @GetMapping
    public Map<String, String> apiRoot() {
        return Map.of(
                "service", "StarVoice Lanka API",
                "group", "2026-Y2-S1-KU-03",
                "docs", "/api/health"
        );
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "service", "starvoice-lanka",
                "time", Instant.now().toString()
        ));
    }
}
