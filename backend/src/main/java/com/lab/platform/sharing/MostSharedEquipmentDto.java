package com.lab.platform.sharing;

import java.math.BigDecimal;

public class MostSharedEquipmentDto {

    private Long equipmentId;
    private String equipmentName;
    private String category;
    private String departmentName;
    private long externalHoursUsed;
    private int externalBookingsCount;
    private BigDecimal revenueGenerated = BigDecimal.ZERO;
    private double externalUtilizationRate;

    public MostSharedEquipmentDto() {}

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public long getExternalHoursUsed() {
        return externalHoursUsed;
    }

    public void setExternalHoursUsed(long externalHoursUsed) {
        this.externalHoursUsed = externalHoursUsed;
    }

    public int getExternalBookingsCount() {
        return externalBookingsCount;
    }

    public void setExternalBookingsCount(int externalBookingsCount) {
        this.externalBookingsCount = externalBookingsCount;
    }

    public BigDecimal getRevenueGenerated() {
        return revenueGenerated;
    }

    public void setRevenueGenerated(BigDecimal revenueGenerated) {
        this.revenueGenerated = revenueGenerated;
    }

    public double getExternalUtilizationRate() {
        return externalUtilizationRate;
    }

    public void setExternalUtilizationRate(double externalUtilizationRate) {
        this.externalUtilizationRate = externalUtilizationRate;
    }
}
