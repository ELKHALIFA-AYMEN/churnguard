package com.churnguard.backend.dto;

import java.time.Instant;

/** Toutes les données conservées sur un client, pour répondre à une demande d'accès (RGPD). */
public record CustomerExport(
        Instant exportedAt,
        CustomerResponse customer,
        PredictionResponse prediction) {
}