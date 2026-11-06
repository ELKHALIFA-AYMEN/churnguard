package com.churnguard.backend.dto;

public record PredictionRunReport(int customers, int created, int updated, int atRisk) {
}