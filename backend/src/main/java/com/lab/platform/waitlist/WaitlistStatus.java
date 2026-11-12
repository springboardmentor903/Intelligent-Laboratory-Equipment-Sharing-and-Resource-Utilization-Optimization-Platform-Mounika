package com.lab.platform.waitlist;

public enum WaitlistStatus {
    WAITING("Waiting in Queue"),
    OFFERED("Slot Offered"),
    ACCEPTED("Offer Accepted"),
    DECLINED("Offer Declined"),
    EXPIRED("Offer Expired"),
    CANCELLED("Left Waitlist");

    private final String displayName;

    WaitlistStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == WAITING || this == OFFERED;
    }
}
