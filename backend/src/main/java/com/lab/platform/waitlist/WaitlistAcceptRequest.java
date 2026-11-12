package com.lab.platform.waitlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WaitlistAcceptRequest {

    @NotBlank(message = "Booking purpose is mandatory")
    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    private String purpose;

    private String notes;

    public WaitlistAcceptRequest() {}

    public WaitlistAcceptRequest(String purpose) {
        this.purpose = purpose;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
