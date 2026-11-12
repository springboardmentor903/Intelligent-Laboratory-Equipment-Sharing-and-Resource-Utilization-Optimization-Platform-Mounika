package com.lab.platform.booking;

import java.time.LocalDateTime;

public class OccupiedTimeRange {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String occupancyType; // "BOOKING_PENDING", "BOOKING_CONFIRMED", "BOOKING_IN_USE", "WAITLIST_HELD"
    private Long referenceId;

    public OccupiedTimeRange() {}

    public OccupiedTimeRange(LocalDateTime startTime, LocalDateTime endTime, String occupancyType, Long referenceId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.occupancyType = occupancyType;
        this.referenceId = referenceId;
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

    public String getOccupancyType() {
        return occupancyType;
    }

    public void setOccupancyType(String occupancyType) {
        this.occupancyType = occupancyType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return this.startTime.isBefore(otherEnd) && this.endTime.isAfter(otherStart);
    }
}
