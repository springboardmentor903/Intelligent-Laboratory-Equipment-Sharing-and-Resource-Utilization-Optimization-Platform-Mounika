package com.lab.platform.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @Query("SELECT b FROM Booking b WHERE b.equipment.institution.id = :institutionId " +
           "AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByInstitutionAndStatus(
            @Param("institutionId") Long institutionId,
            @Param("status") BookingStatus status
    );

    List<Booking> findByStatusAndStartTimeBefore(BookingStatus status, LocalDateTime threshold);

    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, LocalDateTime threshold);

    Optional<Booking> findFirstByEquipmentIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
            Long equipmentId,
            Collection<BookingStatus> statuses,
            LocalDateTime now
    );

    @Query("SELECT b FROM Booking b WHERE b.equipment.id = :equipmentId " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :end AND b.endTime > :start " +
           "ORDER BY b.startTime ASC")
    List<Booking> findOverlappingBookingsForEquipment(
            @Param("equipmentId") Long equipmentId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT b FROM Booking b WHERE b.equipment.id IN :equipmentIds " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :end AND b.endTime > :start " +
           "ORDER BY b.startTime ASC")
    List<Booking> findOverlappingBookingsForEquipmentIds(
            @Param("equipmentIds") Collection<Long> equipmentIds,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Booking> findByRecurrenceIdOrderByStartTimeAsc(String recurrenceId);

    List<Booking> findByRecurrenceIdAndStatusIn(String recurrenceId, Collection<BookingStatus> statuses);
}
