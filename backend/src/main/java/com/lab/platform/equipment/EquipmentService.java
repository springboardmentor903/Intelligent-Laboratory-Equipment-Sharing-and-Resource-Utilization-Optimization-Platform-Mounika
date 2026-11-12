package com.lab.platform.equipment;

import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ConflictException;
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
import java.util.UUID;
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

        String assetTag = (request.getAssetTag() != null && !request.getAssetTag().trim().isEmpty())
                ? request.getAssetTag().trim().toUpperCase()
                : "EQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (equipmentRepository.existsByAssetTag(assetTag)) {
            throw new ConflictException("An equipment with asset tag '" + assetTag + "' already exists.");
        }

        Equipment equipment = new Equipment();
        equipment.setAssetTag(assetTag);
        copyProperties(request, equipment, institution, department);

        Equipment saved = equipmentRepository.save(equipment);
        logger.info("Equipment registered: {} (Tag: {}, ID: {}) for Department: {}",
                saved.getName(), saved.getAssetTag(), saved.getId(), department.getName());
        return saved;
    }

    @Transactional
    public Equipment updateEquipment(Long id, EquipmentRequest request) {
        Equipment equipment = getById(id);

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + request.getInstitutionId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (request.getAssetTag() != null && !request.getAssetTag().trim().isEmpty()) {
            String tag = request.getAssetTag().trim().toUpperCase();
            if (equipmentRepository.existsByAssetTagAndIdNot(tag, id)) {
                throw new ConflictException("An equipment with asset tag '" + tag + "' already exists.");
            }
            equipment.setAssetTag(tag);
        }

        if (request.getStatus() != null && request.getStatus() != equipment.getStatus()) {
            validateStatusTransition(equipment.getStatus(), request.getStatus(), false);
        }

        copyProperties(request, equipment, institution, department);

        Equipment updated = equipmentRepository.save(equipment);
        logger.info("Equipment updated: {} (ID: {})", updated.getName(), updated.getId());
        return updated;
    }

    @Transactional
    public Equipment updateStatus(Long id, EquipmentStatus newStatus) {
        return updateEquipmentStatusInternal(id, newStatus, false);
    }

    /**
     * Internal status transition helper used by both managers and the automated booking engine.
     */
    @Transactional
    public Equipment updateEquipmentStatusInternal(Long id, EquipmentStatus newStatus, boolean isBookingSystemAction) {
        Equipment equipment = getById(id);
        EquipmentStatus oldStatus = equipment.getStatus();

        if (oldStatus == newStatus) {
            return equipment;
        }

        validateStatusTransition(oldStatus, newStatus, isBookingSystemAction);

        equipment.setStatus(newStatus);
        Equipment updated = equipmentRepository.save(equipment);
        logger.info("Equipment status changed: {} (ID: {}) from {} to {}", updated.getName(), id, oldStatus, newStatus);
        return updated;
    }

    /**
     * Strict lifecycle status transitions per project specifications:
     * - AVAILABLE <-> UNDER_MAINTENANCE <-> OUT_OF_SERVICE can be changed by managers.
     * - RETIRED can be set from any status, but can NEVER be changed afterwards.
     * - BOOKED is reserved exclusively for the booking engine (check-in/reservations).
     */
    private void validateStatusTransition(EquipmentStatus currentStatus, EquipmentStatus newStatus, boolean isBookingSystemAction) {
        if (currentStatus == EquipmentStatus.RETIRED) {
            throw new BadRequestException("Equipment is permanently retired. Its status cannot be changed.");
        }

        if (newStatus == EquipmentStatus.RETIRED) {
            return; // Permitted from any status
        }

        if (newStatus == EquipmentStatus.BOOKED && !isBookingSystemAction) {
            throw new BadRequestException("Status 'Booked' is reserved for active reservations and can only be set by the booking system.");
        }

        if (currentStatus == EquipmentStatus.BOOKED && !isBookingSystemAction) {
            throw new BadRequestException("Equipment is currently reserved or in use. Status cannot be modified manually while booked.");
        }
    }

    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = getById(id);
        // Equipment with booking or maintenance history is never deleted. It is marked Retired.
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
        dest.setManufacturer(src.getManufacturer());
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
        dest.setPurchaseDate(src.getPurchaseDate());
        dest.setPurchaseCost(src.getPurchaseCost());
        dest.setRequiresApproval(src.isRequiresApproval());
        dest.setRequiresTraining(src.isRequiresTraining());
        dest.setSharableInterInstitution(src.isSharableInterInstitution());
        dest.setDocumentationUrl(src.getDocumentationUrl());
        dest.setImageUrl(src.getImageUrl());
        dest.setInstitution(institution);
        dest.setDepartment(department);
        dest.setLastCalibratedAt(src.getLastCalibratedAt());
        dest.setNextCalibrationDue(src.getNextCalibrationDue());
    }
}
