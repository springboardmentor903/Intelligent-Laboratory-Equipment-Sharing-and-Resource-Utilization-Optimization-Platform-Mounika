package com.lab.platform.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EquipmentRequest {

    @NotBlank(message = "Equipment name is required")
    private String name;

    private String modelNumber;

    private String serialNumber;

    @NotBlank(message = "Category is required")
    private String category;

    private String tags;

    private String description;

    private String specifications;

    private String locationRoom;

    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    private BigDecimal hourlyRate = BigDecimal.ZERO;

    private boolean sharableInterInstitution = false;

    private String documentationUrl;

    private String imageUrl;

    @NotNull(message = "Institution ID is required")
    private Long institutionId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private LocalDate lastCalibratedAt;

    private LocalDate nextCalibrationDue;

    public EquipmentRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getLocationRoom() {
        return locationRoom;
    }

    public void setLocationRoom(String locationRoom) {
        this.locationRoom = locationRoom;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isSharableInterInstitution() {
        return sharableInterInstitution;
    }

    public void setSharableInterInstitution(boolean sharableInterInstitution) {
        this.sharableInterInstitution = sharableInterInstitution;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDate getLastCalibratedAt() {
        return lastCalibratedAt;
    }

    public void setLastCalibratedAt(LocalDate lastCalibratedAt) {
        this.lastCalibratedAt = lastCalibratedAt;
    }

    public LocalDate getNextCalibrationDue() {
        return nextCalibrationDue;
    }

    public void setNextCalibrationDue(LocalDate nextCalibrationDue) {
        this.nextCalibrationDue = nextCalibrationDue;
    }
}
