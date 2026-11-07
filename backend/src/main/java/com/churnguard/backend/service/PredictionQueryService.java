package com.churnguard.backend.service;

import com.churnguard.backend.domain.Customer;
import com.churnguard.backend.domain.Prediction;
import com.churnguard.backend.dto.PageResponse;
import com.churnguard.backend.dto.PredictionResponse;
import com.churnguard.backend.exception.CustomerNotFoundException;
import com.churnguard.backend.repository.PredictionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

@Service
public class PredictionQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PredictionRepository predictionRepository;
    private final ObjectMapper objectMapper;

    public PredictionQueryService(PredictionRepository predictionRepository,
                                  ObjectMapper objectMapper) {
        this.predictionRepository = predictionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PredictionResponse> list(UUID organizationId, boolean atRiskOnly,
                                                 int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        Page<Prediction> result = atRiskOnly
                ? predictionRepository.findByOrganizationIdAndAtRiskTrueOrderByProbabilityDescIdAsc(
                organizationId, pageable)
                : predictionRepository.findByOrganizationIdOrderByProbabilityDescIdAsc(
                organizationId, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PredictionResponse getForCustomer(UUID organizationId, UUID customerId) {
        return predictionRepository.findByCustomerIdAndOrganizationId(customerId, organizationId)
                .map(this::toResponse)
                .orElseThrow(CustomerNotFoundException::new);
    }
    @Transactional(readOnly = true)
    public Optional<PredictionResponse> findForCustomer(UUID organizationId, UUID customerId) {
        return predictionRepository.findByCustomerIdAndOrganizationId(customerId, organizationId)
                .map(this::toResponse);
    }
    private PredictionResponse toResponse(Prediction prediction) {
        Customer customer = prediction.getCustomer();
        List<PredictionResponse.Factor> factors = objectMapper.readValue(
                prediction.getFactors(), new TypeReference<List<PredictionResponse.Factor>>() {});

        return new PredictionResponse(
                customer.getId(), customer.getExternalId(), customer.getContract(),
                customer.getTenureMonths(), customer.getMonthlyCharges(),
                prediction.getProbability(), prediction.isAtRisk(),
                prediction.getPredictedAt(), factors);
    }
}