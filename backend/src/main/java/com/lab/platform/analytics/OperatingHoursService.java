package com.lab.platform.analytics;

import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperatingHoursService {

    private final EquipmentOperatingHoursRepository operatingHoursRepository;
    private final EquipmentRepository equipmentRepository;

    public OperatingHoursService(EquipmentOperatingHoursRepository operatingHoursRepository,
                                 EquipmentRepository equipmentRepository) {
        this.operatingHoursRepository = operatingHoursRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Retrieves configured operating hours or returns default (Mon-Fri 09:00 - 17:00).
     */
    @Transactional(readOnly = true)
    public List<EquipmentOperatingHours> getOperatingHours(Long equipmentId) {
        List<EquipmentOperatingHours> configured = operatingHoursRepository.findByEquipmentIdAndActiveTrue(equipmentId);
        if (!configured.isEmpty()) {
            return configured;
        }

        // Return default Mon-Fri 09:00 to 17:00
        Equipment equipment = equipmentRepository.findById(equipmentId).orElse(null);
        List<EquipmentOperatingHours> defaults = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            defaults.add(new EquipmentOperatingHours(equipment, day, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        }
        return defaults;
    }

    @Transactional
    public List<EquipmentOperatingHours> setOperatingHours(Long equipmentId, List<OperatingHoursDto> dtos) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + equipmentId));

        operatingHoursRepository.deleteByEquipmentId(equipmentId);

        List<EquipmentOperatingHours> entities = new ArrayList<>();
        for (OperatingHoursDto dto : dtos) {
            if (dto.getDayOfWeek() < 1 || dto.getDayOfWeek() > 7) {
                throw new BadRequestException("Day of week must be between 1 (Monday) and 7 (Sunday).");
            }
            if (dto.getOpenTime().isAfter(dto.getCloseTime()) || dto.getOpenTime().equals(dto.getCloseTime())) {
                throw new BadRequestException("Open time must be strictly before close time for day " + dto.getDayOfWeek());
            }
            EquipmentOperatingHours h = new EquipmentOperatingHours(
                    equipment,
                    dto.getDayOfWeek(),
                    dto.getOpenTime(),
                    dto.getCloseTime()
            );
            entities.add(h);
        }

        return operatingHoursRepository.saveAll(entities);
    }

    /**
     * Calculates total available operating minutes for an equipment between start and end date-times.
     */
    @Transactional(readOnly = true)
    public long calculateAvailableMinutes(Long equipmentId, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            return 0;
        }

        List<EquipmentOperatingHours> hours = getOperatingHours(equipmentId);
        Map<Integer, EquipmentOperatingHours> hoursByDay = hours.stream()
                .collect(Collectors.toMap(EquipmentOperatingHours::getDayOfWeek, h -> h, (a, b) -> a));

        long totalAvailableMinutes = 0;
        LocalDate curDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!curDate.isAfter(endDate)) {
            int dayOfWeek = curDate.getDayOfWeek().getValue(); // 1 = Monday ... 7 = Sunday
            EquipmentOperatingHours openHours = hoursByDay.get(dayOfWeek);

            if (openHours != null) {
                LocalDateTime dayOpen = LocalDateTime.of(curDate, openHours.getOpenTime());
                LocalDateTime dayClose = LocalDateTime.of(curDate, openHours.getCloseTime());

                LocalDateTime slotStart = start.isAfter(dayOpen) ? start : dayOpen;
                LocalDateTime slotEnd = end.isBefore(dayClose) ? end : dayClose;

                if (slotEnd.isAfter(slotStart)) {
                    totalAvailableMinutes += Duration.between(slotStart, slotEnd).toMinutes();
                }
            }
            curDate = curDate.plusDays(1);
        }

        return totalAvailableMinutes;
    }

    /**
     * Calculates weekly capacity in minutes (Mon-Sun).
     */
    @Transactional(readOnly = true)
    public long calculateWeeklyCapacityMinutes(Long equipmentId, LocalDate anyDateInWeek) {
        LocalDate monday = anyDateInWeek.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = LocalDateTime.of(monday, LocalTime.MIN);
        LocalDateTime weekEnd = LocalDateTime.of(monday.plusDays(6), LocalTime.MAX);
        return calculateAvailableMinutes(equipmentId, weekStart, weekEnd);
    }

    /**
     * Calculates how many minutes of an interval [sessionStart, sessionEnd] fall within operating hours.
     */
    public long calculateMinutesWithinOperatingHours(Long equipmentId, LocalDateTime sessionStart, LocalDateTime sessionEnd,
                                                    Map<Integer, EquipmentOperatingHours> hoursByDay) {
        if (sessionStart.isAfter(sessionEnd) || sessionStart.isEqual(sessionEnd)) {
            return 0;
        }

        long minutes = 0;
        LocalDate curDate = sessionStart.toLocalDate();
        LocalDate endDate = sessionEnd.toLocalDate();

        while (!curDate.isAfter(endDate)) {
            int dayOfWeek = curDate.getDayOfWeek().getValue();
            EquipmentOperatingHours openHours = hoursByDay.get(dayOfWeek);

            if (openHours != null) {
                LocalDateTime dayOpen = LocalDateTime.of(curDate, openHours.getOpenTime());
                LocalDateTime dayClose = LocalDateTime.of(curDate, openHours.getCloseTime());

                LocalDateTime effectiveStart = sessionStart.isAfter(dayOpen) ? sessionStart : dayOpen;
                LocalDateTime effectiveEnd = sessionEnd.isBefore(dayClose) ? sessionEnd : dayClose;

                if (effectiveEnd.isAfter(effectiveStart)) {
                    minutes += Duration.between(effectiveStart, effectiveEnd).toMinutes();
                }
            }
            curDate = curDate.plusDays(1);
        }
        return minutes;
    }
}
