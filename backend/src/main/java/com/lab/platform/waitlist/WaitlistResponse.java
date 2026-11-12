package com.lab.platform.waitlist;

import java.time.Duration;
import java.time.LocalDateTime;

public class WaitlistResponse {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String locationRoom;
    private Long userId;
    private String userFullName;
    private LocalDateTime requestedStartTime;
    private LocalDateTime requestedEndTime;
    private WaitlistStatus status;
    private long queuePosition;
    private LocalDateTime offeredAt;
    private LocalDateTime offerExpiresAt;
    private Long remainingSeconds;
    private LocalDateTime createdAt;

    public WaitlistResponse() {}

    public static WaitlistResponse fromEntity(Waitlist w, long queuePosition) {
        WaitlistResponse r = new WaitlistResponse();
        r.setId(w.getId());
        r.setEquipmentId(w.getEquipment().getId());
        r.setEquipmentName(w.getEquipment().getName());
        r.setAssetTag(w.getEquipment().getAssetTag());
        r.setLocationRoom(w.getEquipment().getLocationRoom());
        r.setUserId(w.getUser().getId());
        r.setUserFullName(w.getUser().getFullName());
        r.setRequestedStartTime(w.getRequestedStartTime());
        r.setRequestedEndTime(w.getRequestedEndTime());
        r.setStatus(w.getStatus());
        r.setQueuePosition(queuePosition);
        r.setOfferedAt(w.getOfferedAt());
        r.setOfferExpiresAt(w.getOfferExpiresAt());
        r.setCreatedAt(w.getCreatedAt());

        if (w.getStatus() == WaitlistStatus.OFFERED && w.getOfferExpiresAt() != null) {
            long remaining = Duration.between(LocalDateTime.now(), w.getOfferExpiresAt()).getSeconds();
            r.setRemainingSeconds(Math.max(0, remaining));
        }

        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getLocationRoom() {
        return locationRoom;
    }

    public void setLocationRoom(String locationRoom) {
        this.locationRoom = locationRoom;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public LocalDateTime getRequestedStartTime() {
        return requestedStartTime;
    }

    public void setRequestedStartTime(LocalDateTime requestedStartTime) {
        this.requestedStartTime = requestedStartTime;
    }

    public LocalDateTime getRequestedEndTime() {
        return requestedEndTime;
    }

    public void setRequestedEndTime(LocalDateTime requestedEndTime) {
        this.requestedEndTime = requestedEndTime;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public long getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(long queuePosition) {
        this.queuePosition = queuePosition;
    }

    public LocalDateTime getOfferedAt() {
        return offeredAt;
    }

    public void setOfferedAt(LocalDateTime offeredAt) {
        this.offeredAt = offeredAt;
    }

    public LocalDateTime getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public void setOfferExpiresAt(LocalDateTime offerExpiresAt) {
        this.offerExpiresAt = offerExpiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
