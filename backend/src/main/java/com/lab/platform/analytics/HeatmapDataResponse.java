package com.lab.platform.analytics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HeatmapDataResponse {

    private String platformTimezone;
    private List<HeatmapSlot> slots = new ArrayList<>();

    public static class HeatmapSlot {
        private int dayOfWeek; // 1 = Monday ... 7 = Sunday
        private String dayName;
        private int hourOfDay; // 0 to 23
        private boolean open;
        private long availableMinutes;
        private long usedMinutes;
        private BigDecimal utilizationPercentage; // null if closed, 0 if open & unused, or > 0

        public HeatmapSlot() {}

        public HeatmapSlot(int dayOfWeek, String dayName, int hourOfDay, boolean open,
                           long availableMinutes, long usedMinutes, BigDecimal utilizationPercentage) {
            this.dayOfWeek = dayOfWeek;
            this.dayName = dayName;
            this.hourOfDay = hourOfDay;
            this.open = open;
            this.availableMinutes = availableMinutes;
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

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public long getAvailableMinutes() {
            return availableMinutes;
        }

        public void setAvailableMinutes(long availableMinutes) {
            this.availableMinutes = availableMinutes;
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

    public HeatmapDataResponse() {}

    public HeatmapDataResponse(String platformTimezone) {
        this.platformTimezone = platformTimezone;
    }

    public String getPlatformTimezone() {
        return platformTimezone;
    }

    public void setPlatformTimezone(String platformTimezone) {
        this.platformTimezone = platformTimezone;
    }

    public List<HeatmapSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<HeatmapSlot> slots) {
        this.slots = slots;
    }
}
