package com.churnguard.backend.dto;

public record LoginResponse(String accessToken, String tokenType) {
}