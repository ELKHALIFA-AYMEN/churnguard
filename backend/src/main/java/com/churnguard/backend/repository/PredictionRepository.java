package com.churnguard.backend.repository;

import com.churnguard.backend.domain.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByOrganizationId(UUID organizationId);

    List<Prediction> findByCustomerIdIn(Collection<UUID> customerIds);

    Page<Prediction> findByOrganizationIdOrderByProbabilityDesc(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);
}