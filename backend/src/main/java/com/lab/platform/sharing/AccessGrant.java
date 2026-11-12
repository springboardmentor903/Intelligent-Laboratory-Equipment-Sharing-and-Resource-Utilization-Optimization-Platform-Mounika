package com.lab.platform.sharing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.platform.auth.User;
import com.lab.platform.equipment.Equipment;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_grants")
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agreement_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SharingAgreement agreement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "resetPasswordToken"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Equipment equipment;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AccessGrant() {}

    public AccessGrant(SharingAgreement agreement, User user, Equipment equipment, LocalDate validFrom, LocalDate validTo) {
        this.agreement = agreement;
        this.user = user;
        this.equipment = equipment;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isValidForDate(LocalDate date) {
        if (!active) return false;
        if (date.isBefore(validFrom)) return false;
        if (date.isAfter(validTo)) return false;
        return agreement != null && agreement.getStatus() == SharingAgreementStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SharingAgreement getAgreement() {
        return agreement;
    }

    public void setAgreement(SharingAgreement agreement) {
        this.agreement = agreement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
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
}
