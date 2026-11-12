package com.lab.platform.sharing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.platform.equipment.Equipment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sharing_listings")
public class SharingListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Equipment equipment;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal feePerHour = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer externalSharePercentage = 20; // Default max 20% of weekly capacity

    @Column(nullable = false)
    private Integer minNoticeHours = 24; // Default 24 hours advance notice

    @Column(nullable = false)
    private Integer maxBookingHours = 4; // Default max 4 hours per reservation

    @Column(length = 2000)
    private String termsAndConditions;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SharingListing() {}

    public SharingListing(Equipment equipment, BigDecimal feePerHour, Integer externalSharePercentage,
                          Integer minNoticeHours, Integer maxBookingHours, String termsAndConditions, boolean active) {
        this.equipment = equipment;
        this.feePerHour = feePerHour != null ? feePerHour : BigDecimal.ZERO;
        this.externalSharePercentage = externalSharePercentage != null ? externalSharePercentage : 20;
        this.minNoticeHours = minNoticeHours != null ? minNoticeHours : 24;
        this.maxBookingHours = maxBookingHours != null ? maxBookingHours : 4;
        this.termsAndConditions = termsAndConditions;
        this.active = active;
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

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public BigDecimal getFeePerHour() {
        return feePerHour;
    }

    public void setFeePerHour(BigDecimal feePerHour) {
        this.feePerHour = feePerHour;
    }

    public Integer getExternalSharePercentage() {
        return externalSharePercentage;
    }

    public void setExternalSharePercentage(Integer externalSharePercentage) {
        this.externalSharePercentage = externalSharePercentage;
    }

    public Integer getMinNoticeHours() {
        return minNoticeHours;
    }

    public void setMinNoticeHours(Integer minNoticeHours) {
        this.minNoticeHours = minNoticeHours;
    }

    public Integer getMaxBookingHours() {
        return maxBookingHours;
    }

    public void setMaxBookingHours(Integer maxBookingHours) {
        this.maxBookingHours = maxBookingHours;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
