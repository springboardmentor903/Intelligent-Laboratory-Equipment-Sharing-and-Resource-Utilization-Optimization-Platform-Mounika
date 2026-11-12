package com.lab.platform.booking;

import java.time.LocalDateTime;

/**
 * Event published whenever occupied equipment time is released, allowing
 * the Waitlist module to automatically offer the freed slot to queued users.
 */
public class ReleasedTimeEvent {

    private final Long equipmentId;
    private final LocalDateTime releasedStartTime;
    private final LocalDateTime releasedEndTime;
    private final String reason;

    public ReleasedTimeEvent(Long equipmentId, LocalDateTime releasedStartTime, LocalDateTime releasedEndTime, String reason) {
        this.equipmentId = equipmentId;
        this.releasedStartTime = releasedStartTime;
        this.releasedEndTime = releasedEndTime;
        this.reason = reason;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public LocalDateTime getReleasedStartTime() {
        return releasedStartTime;
    }

    public LocalDateTime getReleasedEndTime() {
        return releasedEndTime;
    }

    public String getReason() {
        return reason;
    }
}
