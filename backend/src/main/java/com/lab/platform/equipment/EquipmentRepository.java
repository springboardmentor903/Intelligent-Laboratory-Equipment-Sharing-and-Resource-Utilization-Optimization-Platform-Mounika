package com.lab.platform.equipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("SELECT e FROM Equipment e WHERE " +
           "(:keyword IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(e.category) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(e.tags) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(e.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:institutionId IS NULL OR e.institution.id = :institutionId) AND " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) " +
           "ORDER BY e.name ASC")
    List<Equipment> findByFilter(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") EquipmentStatus status,
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId
    );

    @Query("SELECT DISTINCT e.category FROM Equipment e WHERE e.category IS NOT NULL ORDER BY e.category ASC")
    List<String> findDistinctCategories();

    List<Equipment> findByInstitutionId(Long institutionId);

    List<Equipment> findByDepartmentId(Long departmentId);
}
