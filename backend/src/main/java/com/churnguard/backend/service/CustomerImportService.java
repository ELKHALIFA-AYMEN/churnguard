package com.churnguard.backend.service;

import com.churnguard.backend.domain.Customer;
import com.churnguard.backend.domain.Organization;
import com.churnguard.backend.dto.ImportReport;
import com.churnguard.backend.repository.CustomerRepository;
import com.churnguard.backend.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerImportService {

    private final CustomerCsvParser parser;
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;

    public CustomerImportService(CustomerCsvParser parser,
                                 CustomerRepository customerRepository,
                                 OrganizationRepository organizationRepository) {
        this.parser = parser;
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public ImportReport importCustomers(UUID organizationId, InputStream input) {
        CustomerCsvParser.ParseResult parsed = parser.parse(input);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalStateException("Organization not found"));

        Map<String, Customer> existing = customerRepository.findAllByOrganizationId(organizationId)
                .stream()
                .collect(Collectors.toMap(Customer::getExternalId, Function.identity()));

        List<Customer> toCreate = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (CustomerCsvParser.ParsedCustomer p : parsed.customers()) {
            Customer fresh = new Customer(
                    organization, p.externalId(), p.tenureMonths(), p.phoneService(),
                    p.multipleLines(), p.internetService(), p.onlineSecurity(), p.onlineBackup(),
                    p.deviceProtection(), p.techSupport(), p.streamingTv(), p.streamingMovies(),
                    p.contract(), p.paperlessBilling(), p.paymentMethod(),
                    p.monthlyCharges(), p.totalCharges(), p.churned());

            Customer current = existing.get(p.externalId());
            if (current == null) {
                toCreate.add(fresh);
                created++;
            } else {
                current.copyValuesFrom(fresh);
                updated++;
            }
        }

        customerRepository.saveAll(toCreate);

        return new ImportReport(
                parsed.totalRows(), created, updated, parsed.rejected(), parsed.errors());
    }
}