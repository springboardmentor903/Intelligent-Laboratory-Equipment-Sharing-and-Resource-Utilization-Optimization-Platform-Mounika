package com.lab.platform.usage;

import com.lab.platform.equipment.EquipmentStatus;
import java.time.LocalDateTime;

public class LiveEquipmentStatusResponse {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String category;
    private String locationRoom;
    private Long institutionId;
    private String institutionName;
    private Long departmentId;
    private String departmentName;
    private EquipmentStatus equipmentStatus;
    private boolean isInUse;

    // Active session details
    private LocalDateTime sessionStartTime;
    private LocalDateTime scheduledEndTime;
    private Long elapsedSeconds;
    private Long elapsedMinutes;

    // Manager-only details (null for regular users)
    private Long currentUserId;
    private String currentUserName;
    private String currentUserEmail;
    private String currentBookingPurpose;
    private Long currentBookingId;

    // Next scheduled booking
    private LocalDateTime nextBookingStartTime;
    private LocalDateTime nextBookingEndTime;

    public LiveEquipmentStatusResponse() {}

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocationRoom() {
        return locationRoom;
    }

    public void setLocationRoom(String locationRoom) {
        this.locationRoom = locationRoom;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public EquipmentStatus getEquipmentStatus() {
        return equipmentStatus;
    }

    public void setEquipmentStatus(EquipmentStatus equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
    }

    public boolean isInUse() {
        return isInUse;
    }

    public void setInUse(boolean inUse) {
        isInUse = inUse;
    }

    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public LocalDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public Long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(Long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public Long getElapsedMinutes() {
        return elapsedMinutes;
    }

    public void setElapsedMinutes(Long elapsedMinutes) {
        this.elapsedMinutes = elapsedMinutes;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public void setCurrentUserEmail(String currentUserEmail) {
        this.currentUserEmail = currentUserEmail;
    }

    public String getCurrentBookingPurpose() {
        return currentBookingPurpose;
    }

    public void setCurrentBookingPurpose(String currentBookingPurpose) {
        this.currentBookingPurpose = currentBookingPurpose;
    }

    public Long getCurrentBookingId() {
        return currentBookingId;
    }

    public void setCurrentBookingId(Long currentBookingId) {
        this.currentBookingId = currentBookingId;
    }

    public LocalDateTime getNextBookingStartTime() {
        return nextBookingStartTime;
    }

    public void setNextBookingStartTime(LocalDateTime nextBookingStartTime) {
        this.nextBookingStartTime = nextBookingStartTime;
    }

    public LocalDateTime getNextBookingEndTime() {
        return nextBookingEndTime;
    }

    public void setNextBookingEndTime(LocalDateTime nextBookingEndTime) {
        this.nextBookingEndTime = nextBookingEndTime;
    }
}
