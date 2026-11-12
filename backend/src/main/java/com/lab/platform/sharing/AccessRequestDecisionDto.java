package com.lab.platform.sharing;

import java.time.LocalDate;

public class AccessRequestDecisionDto {

    private String notes;

    private LocalDate validFrom;

    private LocalDate validTo;

    public AccessRequestDecisionDto() {}

    public AccessRequestDecisionDto(String notes) {
        this.notes = notes;
    }

    public AccessRequestDecisionDto(String notes, LocalDate validFrom, LocalDate validTo) {
        this.notes = notes;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
}
