package com.lab.platform.sharing;

import java.math.BigDecimal;

public class SharingTrendPointDto {

    private String period; // e.g. "Week 1", "Week 2", "2026-10"
    private long hoursProvided;
    private long hoursUsed;
    private BigDecimal feesEarned = BigDecimal.ZERO;
    private BigDecimal feesOwed = BigDecimal.ZERO;

    public SharingTrendPointDto() {}

    public SharingTrendPointDto(String period, long hoursProvided, long hoursUsed, BigDecimal feesEarned, BigDecimal feesOwed) {
        this.period = period;
        this.hoursProvided = hoursProvided;
        this.hoursUsed = hoursUsed;
        this.feesEarned = feesEarned != null ? feesEarned : BigDecimal.ZERO;
        this.feesOwed = feesOwed != null ? feesOwed : BigDecimal.ZERO;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public long getHoursProvided() {
        return hoursProvided;
    }

    public void setHoursProvided(long hoursProvided) {
        this.hoursProvided = hoursProvided;
    }

    public long getHoursUsed() {
        return hoursUsed;
    }

    public void setHoursUsed(long hoursUsed) {
        this.hoursUsed = hoursUsed;
    }

    public BigDecimal getFeesEarned() {
        return feesEarned;
    }

    public void setFeesEarned(BigDecimal feesEarned) {
        this.feesEarned = feesEarned;
    }

    public BigDecimal getFeesOwed() {
        return feesOwed;
    }

    public void setFeesOwed(BigDecimal feesOwed) {
        this.feesOwed = feesOwed;
    }
}
