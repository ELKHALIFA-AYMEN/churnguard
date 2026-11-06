package com.churnguard.backend.controller;

import com.churnguard.backend.dto.PredictionRunReport;
import com.churnguard.backend.service.PredictionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PredictionRunReport run(@AuthenticationPrincipal Jwt jwt) {
        return predictionService.run(UUID.fromString(jwt.getClaimAsString("org")));
    }
}