package com.churnguard.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeResponse(String userId, String organizationId, String role) {
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("org"),
                jwt.getClaimAsString("role"));
    }
}