package com.lab.platform.equipment;

import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Equipment>>> listEquipment(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long departmentId) {
        List<Equipment> result = equipmentService.searchEquipment(keyword, category, status, institutionId, departmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> getEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_LAB_TECHNICIAN', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Equipment>> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        Equipment created = equipmentService.createEquipment(request);
        return ResponseEntity.ok(ApiResponse.ok("Equipment registered successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_LAB_TECHNICIAN', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Equipment>> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        Equipment updated = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Equipment updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_LAB_TECHNICIAN', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Equipment>> updateStatus(@PathVariable Long id, @RequestParam EquipmentStatus status) {
        Equipment updated = equipmentService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(ApiResponse.ok("Equipment retired successfully"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getAllCategories()));
    }

    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getStatuses() {
        return ResponseEntity.ok(ApiResponse.ok(equipmentService.getAllStatuses()));
    }
}
