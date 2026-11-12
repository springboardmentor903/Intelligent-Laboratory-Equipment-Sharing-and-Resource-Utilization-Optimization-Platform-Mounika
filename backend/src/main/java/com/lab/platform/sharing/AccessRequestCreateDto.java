package com.lab.platform.sharing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AccessRequestCreateDto {

    @NotNull(message = "Sharing listing ID is required")
    private Long listingId;

    @NotBlank(message = "Research purpose is mandatory")
    private String purpose;

    @NotNull(message = "Estimated usage hours is required")
    @Min(value = 1, message = "Estimated usage hours must be at least 1")
    private Integer estimatedUsageHours = 10;

    public AccessRequestCreateDto() {}

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getEstimatedUsageHours() {
        return estimatedUsageHours;
    }

    public void setEstimatedUsageHours(Integer estimatedUsageHours) {
        this.estimatedUsageHours = estimatedUsageHours;
    }
}
