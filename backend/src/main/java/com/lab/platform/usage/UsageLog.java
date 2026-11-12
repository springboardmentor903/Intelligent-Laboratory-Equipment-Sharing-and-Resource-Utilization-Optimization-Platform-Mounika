package com.lab.platform.usage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.platform.auth.User;
import com.lab.platform.booking.Booking;
import com.lab.platform.equipment.Equipment;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_logs")
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "resetPasswordToken"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Booking booking;

    @Column(nullable = false)
    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageType usageType = UsageType.INTERNAL;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal feeCharged;

    @Column(length = 1000)
    private String remarks;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum UsageType {
        INTERNAL,
        SHARED
    }

    public UsageLog() {}

    public UsageLog(Equipment equipment, User user, Booking booking, LocalDateTime actualStartTime, UsageType usageType) {
        this.equipment = equipment;
        this.user = user;
        this.booking = booking;
        this.actualStartTime = actualStartTime;
        this.usageType = usageType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public java.math.BigDecimal getFeeCharged() {
        return feeCharged;
    }

    public void setFeeCharged(java.math.BigDecimal feeCharged) {
        this.feeCharged = feeCharged;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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

    /**
     * Calculates the duration in minutes from actualStartTime to actualEndTime (or now if active session).
     */
    public long calculateDurationMinutes(LocalDateTime referenceTime) {
        if (actualStartTime == null) {
            return 0;
        }
        LocalDateTime end = (actualEndTime != null) ? actualEndTime : referenceTime;
        if (end.isBefore(actualStartTime)) {
            return 0;
        }
        return Duration.between(actualStartTime, end).toMinutes();
    }
}
