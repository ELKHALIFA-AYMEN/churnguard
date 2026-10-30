package com.churnguard.backend.dto;

import com.churnguard.backend.domain.Customer;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String externalId,
        int tenureMonths,
        boolean phoneService,
        String multipleLines,
        String internetService,
        String onlineSecurity,
        String onlineBackup,
        String deviceProtection,
        String techSupport,
        String streamingTv,
        String streamingMovies,
        String contract,
        boolean paperlessBilling,
        String paymentMethod,
        BigDecimal monthlyCharges,
        BigDecimal totalCharges,
        Boolean churned
) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getExternalId(), c.getTenureMonths(), c.isPhoneService(),
                c.getMultipleLines(), c.getInternetService(), c.getOnlineSecurity(),
                c.getOnlineBackup(), c.getDeviceProtection(), c.getTechSupport(),
                c.getStreamingTv(), c.getStreamingMovies(), c.getContract(),
                c.isPaperlessBilling(), c.getPaymentMethod(), c.getMonthlyCharges(),
                c.getTotalCharges(), c.getChurned());
    }
}