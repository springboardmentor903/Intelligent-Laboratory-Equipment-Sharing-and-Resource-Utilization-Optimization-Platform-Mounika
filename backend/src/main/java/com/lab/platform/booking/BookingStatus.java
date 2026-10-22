package com.lab.platform.booking;

/**
 * Lifecycle statuses of bookings and reservations as specified:
 * - PENDING_APPROVAL: Requested by researcher, awaiting lab manager review
 * - CONFIRMED: Approved and reserved on calendar
 * - IN_USE: Session currently active (checked in)
 * - COMPLETED: Session concluded (checked out)
 * - CANCELLED: Withdrawn by user or rejected by manager
 * - NO_SHOW: Reserved slot passed without user check-in
 */
public enum BookingStatus {
    PENDING_APPROVAL("Pending Approval"),
    CONFIRMED("Confirmed"),
    IN_USE("In Use"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
