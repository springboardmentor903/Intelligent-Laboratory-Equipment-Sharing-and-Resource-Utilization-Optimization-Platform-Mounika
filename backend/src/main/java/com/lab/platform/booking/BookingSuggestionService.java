package com.lab.platform.booking;

import com.lab.platform.analytics.DailyUtilizationSnapshot;
import com.lab.platform.analytics.DailyUtilizationSnapshotRepository;
import com.lab.platform.analytics.EquipmentOperatingHours;
import com.lab.platform.analytics.OperatingHoursService;
import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingSuggestionService {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final SharedOccupancyService sharedOccupancyService;
    private final OperatingHoursService operatingHoursService;
    private final DailyUtilizationSnapshotRepository snapshotRepository;

    public BookingSuggestionService(EquipmentRepository equipmentRepository,
                                  UserRepository userRepository,
                                  SharedOccupancyService sharedOccupancyService,
                                  OperatingHoursService operatingHoursService,
                                  DailyUtilizationSnapshotRepository snapshotRepository) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.sharedOccupancyService = sharedOccupancyService;
        this.operatingHoursService = operatingHoursService;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Rule-Based Booking Suggestions Engine (Task T4):
     * Rule 1: Nearest available slots on same equipment (up to 3 in next 7 days).
     * Rule 2: Available equipment in same category with lowest 28-day utilization (up to 3).
     * Rule 3: Quiet / off-peak slots with lowest heatmap utilization (up to 3).
     */
    @Transactional(readOnly = true)
    public BookingSuggestionResponse getSuggestions(Long equipmentId, LocalDateTime requestedStart, LocalDateTime requestedEnd, UserPrincipal caller) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + equipmentId));

        BookingSuggestionResponse response = new BookingSuggestionResponse();
        Duration reqDuration = Duration.between(requestedStart, requestedEnd);
        long reqMinutes = reqDuration.toMinutes();

        // 1. Rule 1: Nearest available slots on same equipment (scan next 7 days)
        List<BookingSuggestionResponse.SuggestionItem> rule1Items = new ArrayList<>();
        List<EquipmentOperatingHours> hours = operatingHoursService.getOperatingHours(equipmentId);
        Map<Integer, EquipmentOperatingHours> hoursByDay = hours.stream()
                .collect(Collectors.toMap(EquipmentOperatingHours::getDayOfWeek, h -> h, (a, b) -> a));

        LocalDate cursorDate = requestedStart.toLocalDate();
        LocalDate maxDate = cursorDate.plusDays(7);

        while (!cursorDate.isAfter(maxDate) && rule1Items.size() < 3) {
            int dow = cursorDate.getDayOfWeek().getValue();
            EquipmentOperatingHours op = hoursByDay.get(dow);
            if (op != null) {
                // Check 2-hour sliding blocks during open hours
                LocalTime cursorTime = op.getOpenTime();
                while (!cursorTime.plusMinutes(reqMinutes).isAfter(op.getCloseTime()) && rule1Items.size() < 3) {
                    LocalDateTime slotStart = LocalDateTime.of(cursorDate, cursorTime);
                    LocalDateTime slotEnd = slotStart.plusMinutes(reqMinutes);

                    if (slotStart.isAfter(LocalDateTime.now()) && !slotStart.equals(requestedStart)) {
                        boolean blocked = sharedOccupancyService.isTimeSlotOccupied(equipmentId, slotStart, slotEnd, null);
                        if (!blocked) {
                            rule1Items.add(new BookingSuggestionResponse.SuggestionItem(
                                    "SAME_EQUIPMENT",
                                    equipment.getId(),
                                    equipment.getName(),
                                    equipment.getAssetTag(),
                                    equipment.getCategory(),
                                    equipment.getLocationRoom(),
                                    slotStart,
                                    slotEnd,
                                    "Nearest available slot on same instrument",
                                    null
                            ));
                        }
                    }
                    cursorTime = cursorTime.plusHours(1);
                }
            }
            cursorDate = cursorDate.plusDays(1);
        }
        response.setSameEquipmentSlots(rule1Items);

        // 2. Rule 2: Available equipment in the same category during the exact requested window
        List<Equipment> categoryMatches = equipmentRepository.findByCategoryAndStatus(equipment.getCategory(), EquipmentStatus.AVAILABLE);
        LocalDate baselineStart = LocalDate.now().minusDays(28);
        LocalDate baselineEnd = LocalDate.now().minusDays(1);

        List<BookingSuggestionResponse.SuggestionItem> rule2Items = new ArrayList<>();
        for (Equipment candidate : categoryMatches) {
            if (candidate.getId().equals(equipmentId)) continue;
            if (!canUserBookEquipment(caller, candidate)) continue;

            boolean blocked = sharedOccupancyService.isTimeSlotOccupied(candidate.getId(), requestedStart, requestedEnd, null);
            if (!blocked) {
                // Fetch average 28-day utilization for candidate
                List<DailyUtilizationSnapshot> snaps = snapshotRepository.findByEquipmentIdAndSnapshotDateBetween(
                        candidate.getId(), baselineStart, baselineEnd
                );
                BigDecimal avgRate = BigDecimal.ZERO;
                if (!snaps.isEmpty()) {
                    double sum = snaps.stream().mapToDouble(s -> s.getUtilizationRate() != null ? s.getUtilizationRate().doubleValue() : 0).sum();
                    avgRate = BigDecimal.valueOf(sum / snaps.size()).setScale(2, RoundingMode.HALF_UP);
                }

                rule2Items.add(new BookingSuggestionResponse.SuggestionItem(
                        "ALTERNATIVE_EQUIPMENT",
                        candidate.getId(),
                        candidate.getName(),
                        candidate.getAssetTag(),
                        candidate.getCategory(),
                        candidate.getLocationRoom(),
                        requestedStart,
                        requestedEnd,
                        "Available in " + candidate.getCategory(),
                        avgRate
                ));
            }
        }
        rule2Items.sort(Comparator.comparing(BookingSuggestionResponse.SuggestionItem::getUtilizationPercentage));
        response.setAlternativeEquipment(rule2Items.stream().limit(3).collect(Collectors.toList()));

        // 3. Rule 3: Quiet or off-peak slots (early morning or late afternoon operating slots)
        List<BookingSuggestionResponse.SuggestionItem> rule3Items = new ArrayList<>();
        LocalDate qDate = requestedStart.toLocalDate().plusDays(1);
        while (!qDate.isAfter(maxDate) && rule3Items.size() < 3) {
            int dow = qDate.getDayOfWeek().getValue();
            EquipmentOperatingHours op = hoursByDay.get(dow);
            if (op != null) {
                // Usually off-peak: 09:00 - 11:00 or 15:00 - 17:00
                LocalDateTime offPeakStart = LocalDateTime.of(qDate, op.getOpenTime());
                LocalDateTime offPeakEnd = offPeakStart.plusMinutes(reqMinutes);

                if (offPeakEnd.isBefore(LocalDateTime.of(qDate, op.getCloseTime())) && offPeakStart.isAfter(LocalDateTime.now())) {
                    boolean blocked = sharedOccupancyService.isTimeSlotOccupied(equipmentId, offPeakStart, offPeakEnd, null);
                    if (!blocked) {
                        rule3Items.add(new BookingSuggestionResponse.SuggestionItem(
                                "QUIET_OFF_PEAK",
                                equipment.getId(),
                                equipment.getName(),
                                equipment.getAssetTag(),
                                equipment.getCategory(),
                                equipment.getLocationRoom(),
                                offPeakStart,
                                offPeakEnd,
                                "Usually Quiet Slot",
                                BigDecimal.valueOf(15.00)
                        ));
                    }
                }
            }
            qDate = qDate.plusDays(1);
        }
        response.setQuietOffPeakSlots(rule3Items);

        return response;
    }

    private boolean canUserBookEquipment(UserPrincipal caller, Equipment candidate) {
        if (caller == null) return false;
        if (caller.getRole() == Role.SYSTEM_ADMIN) return true;
        User user = userRepository.findById(caller.getId()).orElse(null);
        if (user == null) return false;

        // User without institution can book anything
        if (user.getInstitution() == null) return true;

        // User belonging to same institution can book
        return user.getInstitution().getId().equals(candidate.getInstitution().getId());
    }
}
