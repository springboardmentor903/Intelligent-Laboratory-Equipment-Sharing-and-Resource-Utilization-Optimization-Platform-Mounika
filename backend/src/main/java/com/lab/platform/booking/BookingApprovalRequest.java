package com.lab.platform.booking;

public class BookingApprovalRequest {

    private boolean approved;
    private String reason;

    public BookingApprovalRequest() {}

    public BookingApprovalRequest(boolean approved, String reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
