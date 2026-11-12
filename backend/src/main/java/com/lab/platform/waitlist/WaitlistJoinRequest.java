package com.lab.platform.waitlist;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class WaitlistJoinRequest {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    public WaitlistJoinRequest() {}

    public WaitlistJoinRequest(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        this.equipmentId = equipmentId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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
}
