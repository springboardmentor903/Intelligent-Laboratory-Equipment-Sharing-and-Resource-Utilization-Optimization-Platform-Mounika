package com.lab.platform.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    List<UsageLog> findByBookingId(Long bookingId);

    Optional<UsageLog> findFirstByBookingIdOrderByIdDesc(Long bookingId);

    List<UsageLog> findByEquipmentId(Long equipmentId);

    Optional<UsageLog> findByEquipmentIdAndActualEndTimeIsNull(Long equipmentId);

    List<UsageLog> findByEquipmentIdAndActualStartTimeBetween(Long equipmentId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT u FROM UsageLog u WHERE u.equipment.id = :equipmentId " +
           "AND u.actualStartTime < :end " +
           "AND (u.actualEndTime IS NULL OR u.actualEndTime > :start)")
    List<UsageLog> findOverlappingUsage(
            @Param("equipmentId") Long equipmentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT u FROM UsageLog u WHERE u.equipment.id IN :equipmentIds " +
           "AND u.actualStartTime < :end " +
           "AND (u.actualEndTime IS NULL OR u.actualEndTime > :start)")
    List<UsageLog> findOverlappingUsageForEquipmentIds(
            @Param("equipmentIds") Collection<Long> equipmentIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<UsageLog> findByActualEndTimeIsNull();

    List<UsageLog> findByUserIdOrderByActualStartTimeDesc(Long userId);
}
