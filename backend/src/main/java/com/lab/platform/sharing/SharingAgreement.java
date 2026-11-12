package com.lab.platform.sharing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.platform.institution.Institution;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sharing_agreements")
public class SharingAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String agreementNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_institution_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Institution providerInstitution;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consumer_institution_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Institution consumerInstitution;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal feePerHour = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SharingAgreementStatus status = SharingAgreementStatus.ACTIVE;

    @Column(length = 1000)
    private String billingTerms;

    @Column(length = 1000)
    private String revocationReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SharingAgreement() {}

    public SharingAgreement(String agreementNumber, Institution providerInstitution, Institution consumerInstitution,
                            LocalDate validFrom, LocalDate validTo, BigDecimal feePerHour, String billingTerms) {
        this.agreementNumber = agreementNumber;
        this.providerInstitution = providerInstitution;
        this.consumerInstitution = consumerInstitution;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.feePerHour = feePerHour != null ? feePerHour : BigDecimal.ZERO;
        this.billingTerms = billingTerms;
        this.status = SharingAgreementStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public Institution getProviderInstitution() {
        return providerInstitution;
    }

    public void setProviderInstitution(Institution providerInstitution) {
        this.providerInstitution = providerInstitution;
    }

    public Institution getConsumerInstitution() {
        return consumerInstitution;
    }

    public void setConsumerInstitution(Institution consumerInstitution) {
        this.consumerInstitution = consumerInstitution;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public BigDecimal getFeePerHour() {
        return feePerHour;
    }

    public void setFeePerHour(BigDecimal feePerHour) {
        this.feePerHour = feePerHour;
    }

    public SharingAgreementStatus getStatus() {
        return status;
    }

    public void setStatus(SharingAgreementStatus status) {
        this.status = status;
    }

    public String getBillingTerms() {
        return billingTerms;
    }

    public void setBillingTerms(String billingTerms) {
        this.billingTerms = billingTerms;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
