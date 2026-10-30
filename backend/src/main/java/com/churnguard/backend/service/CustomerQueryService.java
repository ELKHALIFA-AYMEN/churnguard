package com.churnguard.backend.service;

import com.churnguard.backend.domain.Customer;
import com.churnguard.backend.dto.CustomerResponse;
import com.churnguard.backend.dto.PageResponse;
import com.churnguard.backend.exception.CustomerNotFoundException;
import com.churnguard.backend.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID organizationId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Customer> result = customerRepository.findByOrganizationId(
                organizationId, PageRequest.of(safePage, safeSize, Sort.by("externalId")));

        return new PageResponse<>(
                result.getContent().stream().map(CustomerResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID organizationId, UUID customerId) {
        return customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .map(CustomerResponse::from)
                .orElseThrow(CustomerNotFoundException::new);
    }
}