package com.lab.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UtilizationSummaryResponse {

    private String scopeType;
    private Long scopeId;
    private String scopeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String platformTimezone;

    private long totalAvailableMinutes;
    private double totalAvailableHours;
    private long totalUsedMinutes;
    private double totalUsedHours;
    private BigDecimal utilizationRate;

    private long sharedUsedMinutes;
    private BigDecimal sharedUsagePercentage;

    private BigDecimal targetPercentage = new BigDecimal("70.00");
    private BigDecimal targetGap;
    private String targetStatus;

    public UtilizationSummaryResponse() {}

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPlatformTimezone() {
        return platformTimezone;
    }

    public void setPlatformTimezone(String platformTimezone) {
        this.platformTimezone = platformTimezone;
    }

    public long getTotalAvailableMinutes() {
        return totalAvailableMinutes;
    }

    public void setTotalAvailableMinutes(long totalAvailableMinutes) {
        this.totalAvailableMinutes = totalAvailableMinutes;
    }

    public double getTotalAvailableHours() {
        return totalAvailableHours;
    }

    public void setTotalAvailableHours(double totalAvailableHours) {
        this.totalAvailableHours = totalAvailableHours;
    }

    public long getTotalUsedMinutes() {
        return totalUsedMinutes;
    }

    public void setTotalUsedMinutes(long totalUsedMinutes) {
        this.totalUsedMinutes = totalUsedMinutes;
    }

    public double getTotalUsedHours() {
        return totalUsedHours;
    }

    public void setTotalUsedHours(double totalUsedHours) {
        this.totalUsedHours = totalUsedHours;
    }

    public BigDecimal getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(BigDecimal utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public long getSharedUsedMinutes() {
        return sharedUsedMinutes;
    }

    public void setSharedUsedMinutes(long sharedUsedMinutes) {
        this.sharedUsedMinutes = sharedUsedMinutes;
    }

    public BigDecimal getSharedUsagePercentage() {
        return sharedUsagePercentage;
    }

    public void setSharedUsagePercentage(BigDecimal sharedUsagePercentage) {
        this.sharedUsagePercentage = sharedUsagePercentage;
    }

    public BigDecimal getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(BigDecimal targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    public BigDecimal getTargetGap() {
        return targetGap;
    }

    public void setTargetGap(BigDecimal targetGap) {
        this.targetGap = targetGap;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }
}
