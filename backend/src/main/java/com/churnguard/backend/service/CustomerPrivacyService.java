package com.churnguard.backend.service;

import com.churnguard.backend.domain.Customer;
import com.churnguard.backend.dto.CustomerExport;
import com.churnguard.backend.dto.CustomerResponse;
import com.churnguard.backend.dto.PredictionResponse;
import com.churnguard.backend.exception.CustomerNotFoundException;
import com.churnguard.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Droit d'accès (export) et droit à l'effacement pour un client. */
@Service
public class CustomerPrivacyService {

    private static final Logger log = LoggerFactory.getLogger(CustomerPrivacyService.class);

    private final CustomerRepository customerRepository;
    private final PredictionQueryService predictionQueryService;

    public CustomerPrivacyService(CustomerRepository customerRepository,
                                  PredictionQueryService predictionQueryService) {
        this.customerRepository = customerRepository;
        this.predictionQueryService = predictionQueryService;
    }

    @Transactional(readOnly = true)
    public CustomerExport export(UUID organizationId, UUID customerId) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(CustomerNotFoundException::new);
        PredictionResponse prediction = predictionQueryService
                .findForCustomer(organizationId, customerId)
                .orElse(null);
        return new CustomerExport(Instant.now(), CustomerResponse.from(customer), prediction);
    }

    /** La prédiction du client est supprimée par la base (ON DELETE CASCADE). */
    @Transactional
    public void delete(UUID organizationId, UUID customerId) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(CustomerNotFoundException::new);
        customerRepository.delete(customer);
        // Trace de la suppression, sans aucune donnée personnelle (identifiants techniques seulement).
        log.info("Customer {} deleted from organization {}", customerId, organizationId);
    }
}