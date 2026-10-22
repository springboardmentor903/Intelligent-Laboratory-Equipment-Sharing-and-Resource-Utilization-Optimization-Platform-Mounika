package com.lab.platform.equipment;

import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.institution.Department;
import com.lab.platform.institution.DepartmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.institution.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentRepository equipmentRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            InstitutionRepository institutionRepository,
                            DepartmentRepository departmentRepository) {
        this.equipmentRepository = equipmentRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> searchEquipment(String keyword, String category, EquipmentStatus status, Long institutionId, Long departmentId) {
        return equipmentRepository.findByFilter(keyword, category, status, institutionId, departmentId);
    }

    @Transactional(readOnly = true)
    public Equipment getById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));
    }

    @Transactional
    public Equipment createEquipment(EquipmentRequest request) {
        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + request.getInstitutionId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        Equipment equipment = new Equipment();
        copyProperties(request, equipment, institution, department);

        Equipment saved = equipmentRepository.save(equipment);
        logger.info("Equipment registered: {} (ID: {}) for Department: {}", saved.getName(), saved.getId(), department.getName());
        return saved;
    }

    @Transactional
    public Equipment updateEquipment(Long id, EquipmentRequest request) {
        Equipment equipment = getById(id);

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + request.getInstitutionId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        copyProperties(request, equipment, institution, department);

        Equipment updated = equipmentRepository.save(equipment);
        logger.info("Equipment updated: {} (ID: {})", updated.getName(), updated.getId());
        return updated;
    }

    @Transactional
    public Equipment updateStatus(Long id, EquipmentStatus newStatus) {
        Equipment equipment = getById(id);
        EquipmentStatus oldStatus = equipment.getStatus();
        equipment.setStatus(newStatus);
        Equipment updated = equipmentRepository.save(equipment);
        logger.info("Equipment status changed: {} (ID: {}) from {} to {}", updated.getName(), id, oldStatus, newStatus);
        return updated;
    }

    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = getById(id);
        // Soft-delete or retire
        equipment.setStatus(EquipmentStatus.RETIRED);
        equipmentRepository.save(equipment);
        logger.info("Equipment marked as RETIRED: {} (ID: {})", equipment.getName(), id);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return equipmentRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllStatuses() {
        return Arrays.stream(EquipmentStatus.values())
                .map(s -> Map.of("status", s.name(), "displayName", s.getDisplayName()))
                .collect(Collectors.toList());
    }

    private void copyProperties(EquipmentRequest src, Equipment dest, Institution institution, Department department) {
        dest.setName(src.getName());
        dest.setModelNumber(src.getModelNumber());
        dest.setSerialNumber(src.getSerialNumber());
        dest.setCategory(src.getCategory());
        dest.setTags(src.getTags());
        dest.setDescription(src.getDescription());
        dest.setSpecifications(src.getSpecifications());
        dest.setLocationRoom(src.getLocationRoom());
        if (src.getStatus() != null) {
            dest.setStatus(src.getStatus());
        }
        if (src.getHourlyRate() != null) {
            dest.setHourlyRate(src.getHourlyRate());
        }
        dest.setSharableInterInstitution(src.isSharableInterInstitution());
        dest.setDocumentationUrl(src.getDocumentationUrl());
        dest.setImageUrl(src.getImageUrl());
        dest.setInstitution(institution);
        dest.setDepartment(department);
        dest.setLastCalibratedAt(src.getLastCalibratedAt());
        dest.setNextCalibrationDue(src.getNextCalibrationDue());
    }
}
