package com.lab.platform.usage;

import java.time.LocalDateTime;

public class EquipmentStatusUpdateMessage {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String status;
    private boolean isInUse;
    private LocalDateTime currentSessionStartTime;
    private LocalDateTime scheduledEndTime;
    private String changeType;
    private LocalDateTime timestamp = LocalDateTime.now();

    public EquipmentStatusUpdateMessage() {}

    public EquipmentStatusUpdateMessage(Long equipmentId, String equipmentName, String assetTag,
                                        String status, boolean isInUse,
                                        LocalDateTime currentSessionStartTime, LocalDateTime scheduledEndTime,
                                        String changeType) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.status = status;
        this.isInUse = isInUse;
        this.currentSessionStartTime = currentSessionStartTime;
        this.scheduledEndTime = scheduledEndTime;
        this.changeType = changeType;
        this.timestamp = LocalDateTime.now();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isInUse() {
        return isInUse;
    }

    public void setInUse(boolean inUse) {
        isInUse = inUse;
    }

    public LocalDateTime getCurrentSessionStartTime() {
        return currentSessionStartTime;
    }

    public void setCurrentSessionStartTime(LocalDateTime currentSessionStartTime) {
        this.currentSessionStartTime = currentSessionStartTime;
    }

    public LocalDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
