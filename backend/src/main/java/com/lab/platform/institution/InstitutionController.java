package com.lab.platform.institution;

import com.lab.platform.common.ApiResponse;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;

    public InstitutionController(InstitutionRepository institutionRepository, DepartmentRepository departmentRepository) {
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Institution>>> getAllInstitutions() {
        return ResponseEntity.ok(ApiResponse.ok(institutionRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Institution>> createInstitution(@RequestBody Institution institution) {
        if (institution.getName() == null || institution.getName().trim().isEmpty()) {
            throw new BadRequestException("Institution name is required.");
        }
        if (institution.getCode() == null || institution.getCode().trim().isEmpty()) {
            // Generate simple code if not provided
            institution.setCode(institution.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase());
            if (institution.getCode().length() > 10) {
                institution.setCode(institution.getCode().substring(0, 10));
            }
        }
        Institution saved = institutionRepository.save(institution);
        return ResponseEntity.ok(ApiResponse.ok("Institution created successfully", saved));
    }

    @GetMapping("/{institutionId}/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartmentsByInstitution(@PathVariable Long institutionId) {
        return ResponseEntity.ok(ApiResponse.ok(departmentRepository.findByInstitutionId(institutionId)));
    }

    @PostMapping("/{institutionId}/departments")
    public ResponseEntity<ApiResponse<Department>> createDepartment(
            @PathVariable Long institutionId,
            @RequestBody Department department) {
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + institutionId));

        if (department.getName() == null || department.getName().trim().isEmpty()) {
            throw new BadRequestException("Department name is required.");
        }
        if (department.getCode() == null || department.getCode().trim().isEmpty()) {
            department.setCode(department.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase());
            if (department.getCode().length() > 10) {
                department.setCode(department.getCode().substring(0, 10));
            }
        }
        department.setInstitution(inst);
        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(ApiResponse.ok("Department created successfully", saved));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(departmentRepository.findAll()));
    }
}
