package com.lab.platform.sharing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByRequesterIdOrderByRequestedAtDesc(Long requesterId);

    // Outgoing requests: all requests made by members of requesterInstitution
    List<AccessRequest> findByRequesterInstitutionIdOrderByRequestedAtDesc(Long requesterInstitutionId);

    // Incoming requests: all requests targeting equipment owned by host institution
    @Query("SELECT r FROM AccessRequest r WHERE r.listing.equipment.department.institution.id = :hostInstitutionId ORDER BY r.requestedAt DESC")
    List<AccessRequest> findByHostInstitutionId(@Param("hostInstitutionId") Long hostInstitutionId);

    // Specific filtered queries for pending dashboards
    List<AccessRequest> findByRequesterInstitutionIdAndStatus(Long requesterInstitutionId, AccessRequestStatus status);

    @Query("SELECT r FROM AccessRequest r WHERE r.listing.equipment.department.institution.id = :hostInstitutionId AND r.status = :status ORDER BY r.requestedAt DESC")
    List<AccessRequest> findByHostInstitutionIdAndStatus(
            @Param("hostInstitutionId") Long hostInstitutionId,
            @Param("status") AccessRequestStatus status);
}
