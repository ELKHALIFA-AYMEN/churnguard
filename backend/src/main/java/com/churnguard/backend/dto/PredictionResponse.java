package com.churnguard.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PredictionResponse(
        UUID customerId,
        String externalId,
        String contract,
        int tenureMonths,
        BigDecimal monthlyCharges,
        BigDecimal probability,
        boolean atRisk,
        OffsetDateTime predictedAt,
        List<Factor> factors
) {

    public record Factor(String theme, BigDecimal impact, String direction) {
    }
}