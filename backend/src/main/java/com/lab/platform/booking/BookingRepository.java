package com.lab.platform.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    List<Booking> findByEquipmentIdOrderByStartTimeAsc(Long equipmentId);

    @Query("SELECT b FROM Booking b WHERE b.equipment.id = :equipmentId " +
           "AND b.startTime >= :startWindow AND b.endTime <= :endWindow " +
           "AND b.status NOT IN ('CANCELLED', 'NO_SHOW') ORDER BY b.startTime ASC")
    List<Booking> findByEquipmentIdAndWindow(
            @Param("equipmentId") Long equipmentId,
            @Param("startWindow") LocalDateTime startWindow,
            @Param("endWindow") LocalDateTime endWindow
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.equipment.id = :equipmentId " +
           "AND b.status IN :activeStatuses " +
           "AND b.startTime < :endTime AND b.endTime > :startTime " +
           "AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)")
    long countOverlappingBookings(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses,
            @Param("excludeBookingId") Long excludeBookingId
    );

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.equipment.department.id = :departmentId " +
           "AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByDepartmentAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") BookingStatus status
    );
}
