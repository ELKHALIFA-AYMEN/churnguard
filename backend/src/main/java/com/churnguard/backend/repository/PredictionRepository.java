package com.churnguard.backend.repository;

import com.churnguard.backend.domain.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByOrganizationId(UUID organizationId);

    List<Prediction> findByCustomerIdIn(Collection<UUID> customerIds);

    @EntityGraph(attributePaths = "customer")
    Page<Prediction> findByOrganizationIdOrderByProbabilityDescIdAsc(
            UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "customer")
    Page<Prediction> findByOrganizationIdAndAtRiskTrueOrderByProbabilityDescIdAsc(
            UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "customer")
    Optional<Prediction> findByCustomerIdAndOrganizationId(UUID customerId, UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}