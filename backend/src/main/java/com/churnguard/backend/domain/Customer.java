package com.churnguard.backend.domain;

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
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    private String externalId;
    private int tenureMonths;
    private boolean phoneService;
    private String multipleLines;
    private String internetService;
    private String onlineSecurity;
    private String onlineBackup;
    private String deviceProtection;
    private String techSupport;
    private String streamingTv;
    private String streamingMovies;
    private String contract;
    private boolean paperlessBilling;
    private String paymentMethod;
    private BigDecimal monthlyCharges;
    private BigDecimal totalCharges;
    private Boolean churned;
    private OffsetDateTime createdAt;

    protected Customer() {
        // requis par JPA
    }

    public Customer(Organization organization, String externalId, int tenureMonths,
                    boolean phoneService, String multipleLines, String internetService,
                    String onlineSecurity, String onlineBackup, String deviceProtection,
                    String techSupport, String streamingTv, String streamingMovies,
                    String contract, boolean paperlessBilling, String paymentMethod,
                    BigDecimal monthlyCharges, BigDecimal totalCharges, Boolean churned) {
        this.organization = organization;
        this.externalId = externalId;
        this.tenureMonths = tenureMonths;
        this.phoneService = phoneService;
        this.multipleLines = multipleLines;
        this.internetService = internetService;
        this.onlineSecurity = onlineSecurity;
        this.onlineBackup = onlineBackup;
        this.deviceProtection = deviceProtection;
        this.techSupport = techSupport;
        this.streamingTv = streamingTv;
        this.streamingMovies = streamingMovies;
        this.contract = contract;
        this.paperlessBilling = paperlessBilling;
        this.paymentMethod = paymentMethod;
        this.monthlyCharges = monthlyCharges;
        this.totalCharges = totalCharges;
        this.churned = churned;
        this.createdAt = OffsetDateTime.now();
    }

    public String getOnlineBackup() {
        return onlineBackup;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getExternalId() {
        return externalId;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public boolean isPhoneService() {
        return phoneService;
    }

    public String getMultipleLines() {
        return multipleLines;
    }

    public String getOnlineSecurity() {
        return onlineSecurity;
    }

    public String getInternetService() {
        return internetService;
    }

    public String getDeviceProtection() {
        return deviceProtection;
    }

    public String getTechSupport() {
        return techSupport;
    }

    public String getStreamingTv() {
        return streamingTv;
    }

    public String getStreamingMovies() {
        return streamingMovies;
    }

    public String getContract() {
        return contract;
    }

    public boolean isPaperlessBilling() {
        return paperlessBilling;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getMonthlyCharges() {
        return monthlyCharges;
    }

    public BigDecimal getTotalCharges() {
        return totalCharges;
    }

    public Boolean getChurned() {
        return churned;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void copyValuesFrom(Customer other) {
        this.tenureMonths = other.tenureMonths;
        this.phoneService = other.phoneService;
        this.multipleLines = other.multipleLines;
        this.internetService = other.internetService;
        this.onlineSecurity = other.onlineSecurity;
        this.onlineBackup = other.onlineBackup;
        this.deviceProtection = other.deviceProtection;
        this.techSupport = other.techSupport;
        this.streamingTv = other.streamingTv;
        this.streamingMovies = other.streamingMovies;
        this.contract = other.contract;
        this.paperlessBilling = other.paperlessBilling;
        this.paymentMethod = other.paymentMethod;
        this.monthlyCharges = other.monthlyCharges;
        this.totalCharges = other.totalCharges;
        this.churned = other.churned;
    }
}