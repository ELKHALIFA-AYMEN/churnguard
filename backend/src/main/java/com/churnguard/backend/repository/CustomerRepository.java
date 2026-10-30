package com.churnguard.backend.repository;

import com.churnguard.backend.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<Customer> findAllByOrganizationId(UUID organizationId);

    Optional<Customer> findByOrganizationIdAndExternalId(UUID organizationId, String externalId);

    long countByOrganizationId(UUID organizationId);

    Optional<Customer> findByIdAndOrganizationId(UUID id, UUID organizationId);
}