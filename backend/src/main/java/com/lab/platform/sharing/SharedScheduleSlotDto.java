package com.lab.platform.sharing;

import java.time.LocalDateTime;

public class SharedScheduleSlotDto {

    private Long bookingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title; // Masked for privacy unless caller is owner lab manager/admin
    private boolean isExternal;

    public SharedScheduleSlotDto() {}

    public SharedScheduleSlotDto(Long bookingId, LocalDateTime startTime, LocalDateTime endTime, String title, boolean isExternal) {
        this.bookingId = bookingId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.isExternal = isExternal;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }
}
