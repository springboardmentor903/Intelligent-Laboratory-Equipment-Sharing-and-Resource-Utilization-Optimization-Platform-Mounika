package com.lab.platform.analytics;

import java.math.BigDecimal;

public class DemandItemDto {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String departmentName;
    private long totalBookings;
    private long bookedMinutes;
    private long availableMinutes;
    private BigDecimal bookingRate;
    private BigDecimal noShowRate;
    private BigDecimal cancellationRate;
    private long waitlistCount;
    private boolean highDemand;

    public DemandItemDto() {}

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

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getBookedMinutes() {
        return bookedMinutes;
    }

    public void setBookedMinutes(long bookedMinutes) {
        this.bookedMinutes = bookedMinutes;
    }

    public long getAvailableMinutes() {
        return availableMinutes;
    }

    public void setAvailableMinutes(long availableMinutes) {
        this.availableMinutes = availableMinutes;
    }

    public BigDecimal getBookingRate() {
        return bookingRate;
    }

    public void setBookingRate(BigDecimal bookingRate) {
        this.bookingRate = bookingRate;
    }

    public BigDecimal getNoShowRate() {
        return noShowRate;
    }

    public void setNoShowRate(BigDecimal noShowRate) {
        this.noShowRate = noShowRate;
    }

    public BigDecimal getCancellationRate() {
        return cancellationRate;
    }

    public void setCancellationRate(BigDecimal cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public long getWaitlistCount() {
        return waitlistCount;
    }

    public void setWaitlistCount(long waitlistCount) {
        this.waitlistCount = waitlistCount;
    }

    public boolean isHighDemand() {
        return highDemand;
    }

    public void setHighDemand(boolean highDemand) {
        this.highDemand = highDemand;
    }
}
