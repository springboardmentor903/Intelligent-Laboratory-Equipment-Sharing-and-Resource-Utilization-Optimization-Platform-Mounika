package com.lab.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class IdleAlertItem {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String departmentName;
    private BigDecimal idlePercentage;
    private double idleHours;
    private LocalDateTime lastBookingEndDate;

    public IdleAlertItem() {}

    public IdleAlertItem(Long equipmentId, String equipmentName, String assetTag, String departmentName,
                         BigDecimal idlePercentage, double idleHours, LocalDateTime lastBookingEndDate) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.departmentName = departmentName;
        this.idlePercentage = idlePercentage;
        this.idleHours = idleHours;
        this.lastBookingEndDate = lastBookingEndDate;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public BigDecimal getIdlePercentage() {
        return idlePercentage;
    }

    public void setIdlePercentage(BigDecimal idlePercentage) {
        this.idlePercentage = idlePercentage;
    }

    public double getIdleHours() {
        return idleHours;
    }

    public void setIdleHours(double idleHours) {
        this.idleHours = idleHours;
    }

    public LocalDateTime getLastBookingEndDate() {
        return lastBookingEndDate;
    }

    public void setLastBookingEndDate(LocalDateTime lastBookingEndDate) {
        this.lastBookingEndDate = lastBookingEndDate;
    }
}
