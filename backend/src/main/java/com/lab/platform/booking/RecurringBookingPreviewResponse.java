package com.lab.platform.booking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecurringBookingPreviewResponse {

    private int totalOccurrences;
    private int validCount;
    private int conflictCount;
    private boolean allAvailable;
    private List<OccurrenceDto> occurrences = new ArrayList<>();

    public static class OccurrenceDto {
        private int sequenceNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean available;
        private String conflictReason;

        public OccurrenceDto() {}

        public OccurrenceDto(int sequenceNumber, LocalDateTime startTime, LocalDateTime endTime, boolean available, String conflictReason) {
            this.sequenceNumber = sequenceNumber;
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = available;
            this.conflictReason = conflictReason;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
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

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getConflictReason() {
            return conflictReason;
        }

        public void setConflictReason(String conflictReason) {
            this.conflictReason = conflictReason;
        }
    }

    public RecurringBookingPreviewResponse() {}

    public int getTotalOccurrences() {
        return totalOccurrences;
    }

    public void setTotalOccurrences(int totalOccurrences) {
        this.totalOccurrences = totalOccurrences;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(int conflictCount) {
        this.conflictCount = conflictCount;
    }

    public boolean isAllAvailable() {
        return allAvailable;
    }

    public void setAllAvailable(boolean allAvailable) {
        this.allAvailable = allAvailable;
    }

    public List<OccurrenceDto> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(List<OccurrenceDto> occurrences) {
        this.occurrences = occurrences;
    }
}
