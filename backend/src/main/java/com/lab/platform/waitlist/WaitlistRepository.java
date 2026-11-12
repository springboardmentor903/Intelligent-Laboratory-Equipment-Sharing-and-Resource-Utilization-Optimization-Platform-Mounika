package com.lab.platform.waitlist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    List<Waitlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Waitlist> findByUserId(Long userId, Pageable pageable);

    Page<Waitlist> findByUserIdAndStatus(Long userId, WaitlistStatus status, Pageable pageable);

    List<Waitlist> findByEquipmentIdAndStatusOrderByCreatedAtAsc(Long equipmentId, WaitlistStatus status);

    List<Waitlist> findByStatusAndOfferExpiresAtBefore(WaitlistStatus status, LocalDateTime threshold);

    List<Waitlist> findByStatusAndRequestedStartTimeBefore(WaitlistStatus status, LocalDateTime threshold);

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.equipment.id = :equipmentId " +
           "AND w.status = 'WAITING' " +
           "AND w.createdAt < :createdAt " +
           "AND w.requestedStartTime < :endTime AND w.requestedEndTime > :startTime")
    long calculateQueuePosition(
            @Param("equipmentId") Long equipmentId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.equipment.id = :equipmentId " +
           "AND w.user.id = :userId " +
           "AND w.status IN :activeStatuses " +
           "AND w.requestedStartTime < :endTime AND w.requestedEndTime > :startTime")
    long countOverlappingActiveEntries(
            @Param("equipmentId") Long equipmentId,
            @Param("userId") Long userId,
            @Param("activeStatuses") Collection<WaitlistStatus> activeStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT w FROM Waitlist w WHERE w.equipment.id = :equipmentId " +
           "AND w.status = 'OFFERED' " +
           "AND w.offerExpiresAt > :now " +
           "AND w.requestedStartTime < :endTime AND w.requestedEndTime > :startTime " +
           "AND (:excludeWaitlistId IS NULL OR w.id != :excludeWaitlistId)")
    List<Waitlist> findActiveHolds(
            @Param("equipmentId") Long equipmentId,
            @Param("now") LocalDateTime now,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeWaitlistId") Long excludeWaitlistId
    );

    long countByEquipmentIdAndStatus(Long equipmentId, WaitlistStatus status);
}
