package com.churnguard.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_id", insertable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal probability;

    private boolean atRisk;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal threshold;

    @Column(nullable = false, columnDefinition = "text")
    private String factors;

    private OffsetDateTime predictedAt;

    protected Prediction() {
        // requis par JPA
    }

    public Prediction(Organization organization, Customer customer, BigDecimal probability,
                      boolean atRisk, BigDecimal threshold, String factors) {
        this.organization = organization;
        this.customer = customer;
        update(probability, atRisk, threshold, factors);
    }

    public void update(BigDecimal probability, boolean atRisk, BigDecimal threshold, String factors) {
        this.probability = probability;
        this.atRisk = atRisk;
        this.threshold = threshold;
        this.factors = factors;
        this.predictedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public boolean isAtRisk() {
        return atRisk;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public String getFactors() {
        return factors;
    }

    public OffsetDateTime getPredictedAt() {
        return predictedAt;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}