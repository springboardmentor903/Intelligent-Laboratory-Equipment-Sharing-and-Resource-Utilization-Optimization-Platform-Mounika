package com.lab.platform.institution;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentApprovalService departmentApprovalService;

    public DepartmentController(DepartmentApprovalService departmentApprovalService) {
        this.departmentApprovalService = departmentApprovalService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentPendingResponse>>> getPendingDepartments(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<DepartmentPendingResponse> pending = departmentApprovalService.getPendingDepartments(caller);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<String>> reviewDepartmentApproval(
            @PathVariable Long id,
            @RequestBody DepartmentApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal caller) {
        if (request.isApproved()) {
            departmentApprovalService.approveDepartment(id, caller);
            return ResponseEntity.ok(ApiResponse.ok("Department and Department Head account approved and activated successfully."));
        } else {
            departmentApprovalService.rejectDepartment(id, request.getReason(), caller);
            return ResponseEntity.ok(ApiResponse.ok("Department and pending registration rejected."));
        }
    }
}
