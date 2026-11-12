package com.lab.platform.analytics;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_utilization_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"equipment_id", "snapshot_date"})
})
public class DailyUtilizationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    private Long departmentId;

    private Long institutionId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private long availableMinutes = 0;

    @Column(nullable = false)
    private long usedMinutes = 0;

    @Column(precision = 6, scale = 2)
    private BigDecimal utilizationRate;

    private LocalDateTime createdAt = LocalDateTime.now();

    public DailyUtilizationSnapshot() {}

    public DailyUtilizationSnapshot(Long equipmentId, Long departmentId, Long institutionId,
                                    LocalDate snapshotDate, long availableMinutes, long usedMinutes,
                                    BigDecimal utilizationRate) {
        this.equipmentId = equipmentId;
        this.departmentId = departmentId;
        this.institutionId = institutionId;
        this.snapshotDate = snapshotDate;
        this.availableMinutes = availableMinutes;
        this.usedMinutes = usedMinutes;
        this.utilizationRate = utilizationRate;
        this.createdAt = LocalDateTime.now();
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public long getAvailableMinutes() {
        return availableMinutes;
    }

    public void setAvailableMinutes(long availableMinutes) {
        this.availableMinutes = availableMinutes;
    }

    public long getUsedMinutes() {
        return usedMinutes;
    }

    public void setUsedMinutes(long usedMinutes) {
        this.usedMinutes = usedMinutes;
    }

    public BigDecimal getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(BigDecimal utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
