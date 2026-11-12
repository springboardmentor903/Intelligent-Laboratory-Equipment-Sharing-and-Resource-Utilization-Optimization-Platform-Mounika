package com.lab.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IdleTimeAnalysisResponse {

    private long totalAvailableMinutes;
    private double totalAvailableHours;
    private long totalIdleMinutes;
    private double totalIdleHours;
    private BigDecimal idlePercentage;
    private List<DailyIdleRecord> dailyBreakdown = new ArrayList<>();

    public static class DailyIdleRecord {
        private LocalDate date;
        private long availableMinutes;
        private long bookedMinutes;
        private long idleMinutes;
        private BigDecimal idlePercentage;

        public DailyIdleRecord() {}

        public DailyIdleRecord(LocalDate date, long availableMinutes, long bookedMinutes, long idleMinutes, BigDecimal idlePercentage) {
            this.date = date;
            this.availableMinutes = availableMinutes;
            this.bookedMinutes = bookedMinutes;
            this.idleMinutes = idleMinutes;
            this.idlePercentage = idlePercentage;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getAvailableMinutes() {
            return availableMinutes;
        }

        public void setAvailableMinutes(long availableMinutes) {
            this.availableMinutes = availableMinutes;
        }

        public long getBookedMinutes() {
            return bookedMinutes;
        }

        public void setBookedMinutes(long bookedMinutes) {
            this.bookedMinutes = bookedMinutes;
        }

        public long getIdleMinutes() {
            return idleMinutes;
        }

        public void setIdleMinutes(long idleMinutes) {
            this.idleMinutes = idleMinutes;
        }

        public BigDecimal getIdlePercentage() {
            return idlePercentage;
        }

        public void setIdlePercentage(BigDecimal idlePercentage) {
            this.idlePercentage = idlePercentage;
        }
    }

    public IdleTimeAnalysisResponse() {}

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

    public long getTotalIdleMinutes() {
        return totalIdleMinutes;
    }

    public void setTotalIdleMinutes(long totalIdleMinutes) {
        this.totalIdleMinutes = totalIdleMinutes;
    }

    public double getTotalIdleHours() {
        return totalIdleHours;
    }

    public void setTotalIdleHours(double totalIdleHours) {
        this.totalIdleHours = totalIdleHours;
    }

    public BigDecimal getIdlePercentage() {
        return idlePercentage;
    }

    public void setIdlePercentage(BigDecimal idlePercentage) {
        this.idlePercentage = idlePercentage;
    }

    public List<DailyIdleRecord> getDailyBreakdown() {
        return dailyBreakdown;
    }

    public void setDailyBreakdown(List<DailyIdleRecord> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown;
    }
}
