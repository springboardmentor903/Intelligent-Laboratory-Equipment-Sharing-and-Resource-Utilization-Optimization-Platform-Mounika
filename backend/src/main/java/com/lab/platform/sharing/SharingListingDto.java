package com.lab.platform.sharing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SharingListingDto {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Fee per hour is required")
    @DecimalMin(value = "0.0", message = "Fee per hour cannot be negative")
    private BigDecimal feePerHour;

    @NotNull(message = "External share percentage is required")
    @Min(value = 1, message = "Share percentage must be at least 1%")
    @Max(value = 100, message = "Share percentage cannot exceed 100%")
    private Integer externalSharePercentage = 20;

    @NotNull(message = "Minimum notice hours is required")
    @Min(value = 1, message = "Minimum notice must be at least 1 hour")
    private Integer minNoticeHours = 24;

    @NotNull(message = "Maximum booking hours is required")
    @Min(value = 1, message = "Max booking hours must be at least 1 hour")
    private Integer maxBookingHours = 4;

    private String termsAndConditions;

    private boolean active = true;

    public SharingListingDto() {}

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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
}
