package com.lab.platform.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingSuggestionResponse {

    private List<SuggestionItem> sameEquipmentSlots = new ArrayList<>();
    private List<SuggestionItem> alternativeEquipment = new ArrayList<>();
    private List<SuggestionItem> quietOffPeakSlots = new ArrayList<>();

    public static class SuggestionItem {
        private String ruleType; // "SAME_EQUIPMENT", "ALTERNATIVE_EQUIPMENT", "QUIET_OFF_PEAK"
        private Long equipmentId;
        private String equipmentName;
        private String assetTag;
        private String category;
        private String locationRoom;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String label;
        private BigDecimal utilizationPercentage;

        public SuggestionItem() {}

        public SuggestionItem(String ruleType, Long equipmentId, String equipmentName, String assetTag,
                              String category, String locationRoom, LocalDateTime startTime, LocalDateTime endTime,
                              String label, BigDecimal utilizationPercentage) {
            this.ruleType = ruleType;
            this.equipmentId = equipmentId;
            this.equipmentName = equipmentName;
            this.assetTag = assetTag;
            this.category = category;
            this.locationRoom = locationRoom;
            this.startTime = startTime;
            this.endTime = endTime;
            this.label = label;
            this.utilizationPercentage = utilizationPercentage;
        }

        public String getRuleType() {
            return ruleType;
        }

        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getLocationRoom() {
            return locationRoom;
        }

        public void setLocationRoom(String locationRoom) {
            this.locationRoom = locationRoom;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getUtilizationPercentage() {
            return utilizationPercentage;
        }

        public void setUtilizationPercentage(BigDecimal utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
        }
    }

    public BookingSuggestionResponse() {}

    public List<SuggestionItem> getSameEquipmentSlots() {
        return sameEquipmentSlots;
    }

    public void setSameEquipmentSlots(List<SuggestionItem> sameEquipmentSlots) {
        this.sameEquipmentSlots = sameEquipmentSlots;
    }

    public List<SuggestionItem> getAlternativeEquipment() {
        return alternativeEquipment;
    }

    public void setAlternativeEquipment(List<SuggestionItem> alternativeEquipment) {
        this.alternativeEquipment = alternativeEquipment;
    }

    public List<SuggestionItem> getQuietOffPeakSlots() {
        return quietOffPeakSlots;
    }

    public void setQuietOffPeakSlots(List<SuggestionItem> quietOffPeakSlots) {
        this.quietOffPeakSlots = quietOffPeakSlots;
    }
}
