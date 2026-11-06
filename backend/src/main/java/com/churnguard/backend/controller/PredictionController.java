package com.churnguard.backend.controller;

import com.churnguard.backend.dto.PageResponse;
import com.churnguard.backend.dto.PredictionResponse;
import com.churnguard.backend.dto.PredictionRunReport;
import com.churnguard.backend.service.PredictionQueryService;
import com.churnguard.backend.service.PredictionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final PredictionQueryService queryService;

    public PredictionController(PredictionService predictionService,
                                PredictionQueryService queryService) {
        this.predictionService = predictionService;
        this.queryService = queryService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PredictionRunReport run(@AuthenticationPrincipal Jwt jwt) {
        return predictionService.run(organizationId(jwt));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public PageResponse<PredictionResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "false") boolean atRiskOnly,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return queryService.list(organizationId(jwt), atRiskOnly, page, size);
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public PredictionResponse forCustomer(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID customerId) {
        return queryService.getForCustomer(organizationId(jwt), customerId);
    }

    private static UUID organizationId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("org"));
    }
}