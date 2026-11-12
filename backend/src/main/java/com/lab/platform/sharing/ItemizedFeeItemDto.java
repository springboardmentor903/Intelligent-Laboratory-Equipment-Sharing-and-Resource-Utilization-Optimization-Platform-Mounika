package com.lab.platform.sharing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemizedFeeItemDto {

    private Long bookingId;
    private Long usageLogId;
    private String equipmentName;
    private String researcherName;
    private String consumerInstitutionName;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private long durationMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal feeCharged;

    public ItemizedFeeItemDto() {}

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUsageLogId() {
        return usageLogId;
    }

    public void setUsageLogId(Long usageLogId) {
        this.usageLogId = usageLogId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public void setResearcherName(String researcherName) {
        this.researcherName = researcherName;
    }

    public String getConsumerInstitutionName() {
        return consumerInstitutionName;
    }

    public void setConsumerInstitutionName(String consumerInstitutionName) {
        this.consumerInstitutionName = consumerInstitutionName;
    }

    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public LocalDateTime getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(LocalDateTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public BigDecimal getFeeCharged() {
        return feeCharged;
    }

    public void setFeeCharged(BigDecimal feeCharged) {
        this.feeCharged = feeCharged;
    }
}
