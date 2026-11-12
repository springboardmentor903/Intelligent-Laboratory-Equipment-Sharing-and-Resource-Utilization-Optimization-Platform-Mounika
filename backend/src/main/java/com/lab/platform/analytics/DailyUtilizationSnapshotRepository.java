package com.lab.platform.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyUtilizationSnapshotRepository extends JpaRepository<DailyUtilizationSnapshot, Long> {

    Optional<DailyUtilizationSnapshot> findByEquipmentIdAndSnapshotDate(Long equipmentId, LocalDate snapshotDate);

    List<DailyUtilizationSnapshot> findByEquipmentIdAndSnapshotDateBetween(Long equipmentId, LocalDate start, LocalDate end);

    List<DailyUtilizationSnapshot> findByEquipmentIdInAndSnapshotDateBetween(Collection<Long> equipmentIds, LocalDate start, LocalDate end);

    List<DailyUtilizationSnapshot> findByDepartmentIdAndSnapshotDateBetween(Long departmentId, LocalDate start, LocalDate end);

    List<DailyUtilizationSnapshot> findByInstitutionIdAndSnapshotDateBetween(Long institutionId, LocalDate start, LocalDate end);
}
