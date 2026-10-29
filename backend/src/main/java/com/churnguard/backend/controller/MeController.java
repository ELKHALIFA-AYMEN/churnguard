package com.churnguard.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    public record MeResponse(String userId, String organizationId, String role,
                             List<String> authorities) {
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new MeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("org"),
                jwt.getClaimAsString("role"),
                authorities);
    }
}