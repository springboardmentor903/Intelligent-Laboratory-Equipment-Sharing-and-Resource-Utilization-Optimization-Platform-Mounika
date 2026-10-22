package com.lab.platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByResetPasswordToken(String resetPasswordToken);
    List<User> findByRole(Role role);
    List<User> findByInstitutionId(Long institutionId);
    List<User> findByDepartmentId(Long departmentId);
}
