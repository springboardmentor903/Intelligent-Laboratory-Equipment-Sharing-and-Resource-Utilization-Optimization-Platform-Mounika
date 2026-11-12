package com.lab.platform.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentOperatingHoursRepository extends JpaRepository<EquipmentOperatingHours, Long> {

    List<EquipmentOperatingHours> findByEquipmentIdAndActiveTrue(Long equipmentId);

    List<EquipmentOperatingHours> findByEquipmentId(Long equipmentId);

    Optional<EquipmentOperatingHours> findByEquipmentIdAndDayOfWeek(Long equipmentId, int dayOfWeek);

    List<EquipmentOperatingHours> findByEquipmentIdInAndActiveTrue(Collection<Long> equipmentIds);

    void deleteByEquipmentId(Long equipmentId);
}
