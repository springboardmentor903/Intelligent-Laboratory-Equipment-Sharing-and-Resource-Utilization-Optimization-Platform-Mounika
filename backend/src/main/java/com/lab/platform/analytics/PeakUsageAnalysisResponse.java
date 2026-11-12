package com.lab.platform.analytics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeakUsageAnalysisResponse {

    private String busiestDayName;
    private int busiestDayOfWeek; // 1 = Mon ... 7 = Sun
    private Map<Integer, Long> dayOfWeekUsageMinutes = new HashMap<>();

    private int peakHourOfDay; // 0 to 23
    private Map<Integer, Long> hourOfDayUsageMinutes = new HashMap<>();

    private List<PeakSlotDto> top5BusiestSlots = new ArrayList<>();

    public static class PeakSlotDto {
        private int dayOfWeek;
        private String dayName;
        private int hourOfDay;
        private long usedMinutes;
        private BigDecimal utilizationPercentage;

        public PeakSlotDto() {}

        public PeakSlotDto(int dayOfWeek, String dayName, int hourOfDay, long usedMinutes, BigDecimal utilizationPercentage) {
            this.dayOfWeek = dayOfWeek;
            this.dayName = dayName;
            this.hourOfDay = hourOfDay;
            this.usedMinutes = usedMinutes;
            this.utilizationPercentage = utilizationPercentage;
        }

        public int getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(int dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getDayName() {
            return dayName;
        }

        public void setDayName(String dayName) {
            this.dayName = dayName;
        }

        public int getHourOfDay() {
            return hourOfDay;
        }

        public void setHourOfDay(int hourOfDay) {
            this.hourOfDay = hourOfDay;
        }

        public long getUsedMinutes() {
            return usedMinutes;
        }

        public void setUsedMinutes(long usedMinutes) {
            this.usedMinutes = usedMinutes;
        }

        public BigDecimal getUtilizationPercentage() {
            return utilizationPercentage;
        }

        public void setUtilizationPercentage(BigDecimal utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
        }
    }

    public PeakUsageAnalysisResponse() {}

    public String getBusiestDayName() {
        return busiestDayName;
    }

    public void setBusiestDayName(String busiestDayName) {
        this.busiestDayName = busiestDayName;
    }

    public int getBusiestDayOfWeek() {
        return busiestDayOfWeek;
    }

    public void setBusiestDayOfWeek(int busiestDayOfWeek) {
        this.busiestDayOfWeek = busiestDayOfWeek;
    }

    public Map<Integer, Long> getDayOfWeekUsageMinutes() {
        return dayOfWeekUsageMinutes;
    }

    public void setDayOfWeekUsageMinutes(Map<Integer, Long> dayOfWeekUsageMinutes) {
        this.dayOfWeekUsageMinutes = dayOfWeekUsageMinutes;
    }

    public int getPeakHourOfDay() {
        return peakHourOfDay;
    }

    public void setPeakHourOfDay(int peakHourOfDay) {
        this.peakHourOfDay = peakHourOfDay;
    }

    public Map<Integer, Long> getHourOfDayUsageMinutes() {
        return hourOfDayUsageMinutes;
    }

    public void setHourOfDayUsageMinutes(Map<Integer, Long> hourOfDayUsageMinutes) {
        this.hourOfDayUsageMinutes = hourOfDayUsageMinutes;
    }

    public List<PeakSlotDto> getTop5BusiestSlots() {
        return top5BusiestSlots;
    }

    public void setTop5BusiestSlots(List<PeakSlotDto> top5BusiestSlots) {
        this.top5BusiestSlots = top5BusiestSlots;
    }
}
