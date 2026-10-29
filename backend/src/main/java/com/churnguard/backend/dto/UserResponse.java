package com.churnguard.backend.dto;

import com.churnguard.backend.domain.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String role, String organizationName) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getOrganization().getName());
    }
}