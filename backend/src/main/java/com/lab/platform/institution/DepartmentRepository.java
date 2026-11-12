package com.lab.platform.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByInstitutionId(Long institutionId);
    List<Department> findByInstitutionIdAndActiveTrue(Long institutionId);
    List<Department> findByInstitutionIdAndActiveFalse(Long institutionId);
    List<Department> findByActiveFalse();
    List<Department> findByActiveTrue();
    Optional<Department> findByCode(String code);
}
