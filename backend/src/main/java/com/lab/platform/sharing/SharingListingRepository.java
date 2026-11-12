package com.lab.platform.sharing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharingListingRepository extends JpaRepository<SharingListing, Long> {

    Optional<SharingListing> findByEquipmentId(Long equipmentId);

    List<SharingListing> findByActiveTrue();

    @Query("SELECT l FROM SharingListing l WHERE l.active = true " +
           "AND (:institutionId IS NULL OR l.equipment.department.institution.id = :institutionId) " +
           "AND (:category IS NULL OR LOWER(l.equipment.category) = LOWER(:category)) " +
           "AND (:keyword IS NULL OR LOWER(l.equipment.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(l.equipment.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<SharingListing> searchListings(
            @Param("institutionId") Long institutionId,
            @Param("category") String category,
            @Param("keyword") String keyword);
}
