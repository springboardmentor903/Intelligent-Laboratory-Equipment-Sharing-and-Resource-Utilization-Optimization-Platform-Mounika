package com.lab.platform.institution;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentApprovalService.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentApprovalService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentPendingResponse> getPendingDepartments(UserPrincipal caller) {
        verifyAdminAccess(caller);

        List<Department> pendingList;
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            pendingList = departmentRepository.findByActiveFalse();
        } else {
            User admin = userRepository.findById(caller.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (admin.getInstitution() == null) {
                return List.of();
            }
            pendingList = departmentRepository.findByInstitutionIdAndActiveFalse(admin.getInstitution().getId());
        }

        List<DepartmentPendingResponse> results = new ArrayList<>();
        for (Department dept : pendingList) {
            List<User> usersInDept = userRepository.findByDepartmentId(dept.getId());
            User pendingHead = usersInDept.stream()
                    .filter(u -> u.getRole() == Role.DEPARTMENT_HEAD)
                    .findFirst()
                    .orElse(usersInDept.isEmpty() ? null : usersInDept.get(0));

            results.add(new DepartmentPendingResponse(
                    dept.getId(),
                    dept.getName(),
                    dept.getCode(),
                    dept.getInstitution().getId(),
                    dept.getInstitution().getName(),
                    pendingHead != null ? pendingHead.getId() : null,
                    pendingHead != null ? pendingHead.getFullName() : "N/A",
                    pendingHead != null ? pendingHead.getEmail() : "N/A",
                    pendingHead != null ? pendingHead.getPhoneNumber() : null,
                    dept.getCreatedAt()
            ));
        }

        return results;
    }

    @Transactional
    public void approveDepartment(Long departmentId, UserPrincipal caller) {
        verifyAdminAccess(caller);

        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        verifyScope(caller, dept.getInstitution().getId());

        dept.setActive(true);
        departmentRepository.save(dept);

        List<User> usersInDept = userRepository.findByDepartmentId(dept.getId());
        for (User u : usersInDept) {
            if (u.getRole() == Role.DEPARTMENT_HEAD) {
                u.setActive(true);
                userRepository.save(u);
                logger.info("Department Head approved and activated: {} for department: {}", u.getEmail(), dept.getName());
            }
        }
        logger.info("Department activated: {} (ID: {}) by {}", dept.getName(), dept.getId(), caller.getUsername());
    }

    @Transactional
    public void rejectDepartment(Long departmentId, String reason, UserPrincipal caller) {
        verifyAdminAccess(caller);

        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        verifyScope(caller, dept.getInstitution().getId());

        List<User> usersInDept = userRepository.findByDepartmentId(dept.getId());
        for (User u : usersInDept) {
            userRepository.delete(u);
            logger.info("Deleted pending user account on rejection: {}", u.getEmail());
        }

        departmentRepository.delete(dept);
        logger.info("Deleted pending department on rejection: {} (ID: {}). Reason: {}", dept.getName(), departmentId, reason);
    }

    private void verifyAdminAccess(UserPrincipal caller) {
        if (caller.getRole() != Role.SYSTEM_ADMIN && caller.getRole() != Role.INSTITUTION_ADMIN) {
            throw new UnauthorizedException("Only Institution Administrators and System Administrators can review department registrations.");
        }
    }

    private void verifyScope(UserPrincipal caller, Long targetInstitutionId) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            return;
        }
        User admin = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (admin.getInstitution() == null || !admin.getInstitution().getId().equals(targetInstitutionId)) {
            throw new UnauthorizedException("You do not have administrative authority over this institution.");
        }
    }
}
