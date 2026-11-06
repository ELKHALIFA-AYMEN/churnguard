package com.churnguard.backend.service;

import com.churnguard.backend.domain.Customer;
import com.churnguard.backend.domain.Organization;
import com.churnguard.backend.domain.Prediction;
import com.churnguard.backend.dto.PredictionRunReport;
import com.churnguard.backend.ml.MlApi;
import com.churnguard.backend.ml.MlClient;
import com.churnguard.backend.repository.CustomerRepository;
import com.churnguard.backend.repository.OrganizationRepository;
import com.churnguard.backend.repository.PredictionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private final CustomerRepository customerRepository;
    private final PredictionRepository predictionRepository;
    private final OrganizationRepository organizationRepository;
    private final MlClient mlClient;
    private final ObjectMapper objectMapper;

    private final Set<UUID> runningOrganizations = ConcurrentHashMap.newKeySet();

    public PredictionService(CustomerRepository customerRepository,
                             PredictionRepository predictionRepository,
                             OrganizationRepository organizationRepository,
                             MlClient mlClient,
                             ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.predictionRepository = predictionRepository;
        this.organizationRepository = organizationRepository;
        this.mlClient = mlClient;
        this.objectMapper = objectMapper;
    }

    public PredictionRunReport run(UUID organizationId) {
        if (!runningOrganizations.add(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A prediction run is already in progress");
        }
        try {
            return doRun(organizationId);
        } finally {
            runningOrganizations.remove(organizationId);
        }
    }

    private PredictionRunReport doRun(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalStateException("Organization not found"));

        int total = 0;
        int created = 0;
        int updated = 0;
        int atRisk = 0;
        int pageNumber = 0;
        Page<Customer> page;

        do {
            page = customerRepository.findByOrganizationId(
                    organizationId,
                    PageRequest.of(pageNumber++, MlClient.MAX_BATCH, Sort.by("externalId")));
            List<Customer> customers = page.getContent();
            if (customers.isEmpty()) {
                break;
            }

            MlApi.Response response = mlClient.score(
                    customers.stream().map(MlApi.MlCustomer::from).toList());

            Map<UUID, Prediction> existing = predictionRepository
                    .findByCustomerIdIn(customers.stream().map(Customer::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(Prediction::getCustomerId, Function.identity()));

            List<Prediction> toSave = new ArrayList<>();
            for (int i = 0; i < customers.size(); i++) {
                Customer customer = customers.get(i);
                MlApi.Prediction result = response.predictions().get(i);
                String factors = objectMapper.writeValueAsString(result.factors());

                Prediction current = existing.get(customer.getId());
                if (current == null) {
                    toSave.add(new Prediction(organization, customer, result.probability(),
                            result.atRisk(), response.threshold(), factors));
                    created++;
                } else {
                    current.update(result.probability(), result.atRisk(),
                            response.threshold(), factors);
                    toSave.add(current);
                    updated++;
                }
                if (result.atRisk()) {
                    atRisk++;
                }
            }

            predictionRepository.saveAll(toSave);
            total += customers.size();
        } while (page.hasNext());

        return new PredictionRunReport(total, created, updated, atRisk);
    }
}