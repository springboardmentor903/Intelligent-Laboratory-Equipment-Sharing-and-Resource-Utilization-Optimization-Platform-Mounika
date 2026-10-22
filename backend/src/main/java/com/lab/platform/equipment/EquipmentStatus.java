package com.lab.platform.equipment;

/**
 * Lifecycle statuses of laboratory equipment as specified in the project requirements:
 * - AVAILABLE: Operational and ready for reservation
 * - BOOKED: Reserved for an ongoing or scheduled session
 * - UNDER_MAINTENANCE: Scheduled or emergency maintenance in progress
 * - OUT_OF_SERVICE: Temporarily non-operational / awaiting parts or calibration
 * - RETIRED: Decommissioned and no longer bookable
 */
public enum EquipmentStatus {
    AVAILABLE("Available"),
    BOOKED("Booked"),
    UNDER_MAINTENANCE("Under Maintenance"),
    OUT_OF_SERVICE("Out of Service"),
    RETIRED("Retired");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
