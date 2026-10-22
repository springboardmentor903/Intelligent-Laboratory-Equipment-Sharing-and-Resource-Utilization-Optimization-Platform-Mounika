package com.lab.platform.booking;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO designed for direct consumption by FullCalendar.js in the React frontend.
 */
public class BookingCalendarEvent {

    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String backgroundColor;
    private String borderColor;
    private String textColor = "#ffffff";
    private Map<String, Object> extendedProps = new HashMap<>();

    public BookingCalendarEvent() {}

    public BookingCalendarEvent(Booking booking) {
        this.id = booking.getId();
        this.title = booking.getEquipment().getName() + " (" + booking.getUser().getFullName() + ")";
        this.start = booking.getStartTime();
        this.end = booking.getEndTime();

        // Color coding by booking status
        switch (booking.getStatus()) {
            case CONFIRMED:
                this.backgroundColor = "#10b981"; // Emerald green
                this.borderColor = "#059669";
                break;
            case PENDING_APPROVAL:
                this.backgroundColor = "#f59e0b"; // Amber
                this.borderColor = "#d97706";
                break;
            case IN_USE:
                this.backgroundColor = "#3b82f6"; // Blue
                this.borderColor = "#2563eb";
                break;
            case COMPLETED:
                this.backgroundColor = "#6b7280"; // Gray
                this.borderColor = "#4b5563";
                break;
            default:
                this.backgroundColor = "#ef4444"; // Red
                this.borderColor = "#dc2626";
                break;
        }

        extendedProps.put("status", booking.getStatus().name());
        extendedProps.put("statusDisplayName", booking.getStatus().getDisplayName());
        extendedProps.put("purpose", booking.getPurpose());
        extendedProps.put("notes", booking.getNotes());
        extendedProps.put("userName", booking.getUser().getFullName());
        extendedProps.put("userEmail", booking.getUser().getEmail());
        extendedProps.put("equipmentId", booking.getEquipment().getId());
        extendedProps.put("equipmentName", booking.getEquipment().getName());
        extendedProps.put("locationRoom", booking.getEquipment().getLocationRoom());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public Map<String, Object> getExtendedProps() {
        return extendedProps;
    }

    public void setExtendedProps(Map<String, Object> extendedProps) {
        this.extendedProps = extendedProps;
    }
}
