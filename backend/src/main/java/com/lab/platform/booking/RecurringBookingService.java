package com.lab.platform.booking;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ConflictException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class RecurringBookingService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringBookingService.class);

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final SharedOccupancyService sharedOccupancyService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.recurrence.max-occurrences:52}")
    private int maxRecurrenceOccurrences;

    public RecurringBookingService(BookingRepository bookingRepository,
                                   EquipmentRepository equipmentRepository,
                                   UserRepository userRepository,
                                   SharedOccupancyService sharedOccupancyService,
                                   ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.sharedOccupancyService = sharedOccupancyService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Preview recurring booking occurrences without persisting anything.
     */
    @Transactional(readOnly = true)
    public RecurringBookingPreviewResponse previewRecurring(RecurringBookingRequest request, UserPrincipal caller) {
        validateRequest(request, caller);

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.getEquipmentId()));

        List<OccurrenceWindow> rawOccurrences = generateOccurrences(
                request.getFrequency(),
                request.getInterval(),
                request.getWeekdays(),
                request.getStartTime(),
                request.getEndTime(),
                request.getUntilDate(),
                maxRecurrenceOccurrences
        );

        RecurringBookingPreviewResponse response = new RecurringBookingPreviewResponse();
        int valid = 0;
        int conflict = 0;
        int seq = 1;

        for (OccurrenceWindow occ : rawOccurrences) {
            boolean isOccupied = sharedOccupancyService.isTimeSlotOccupied(
                    equipment.getId(), occ.start, occ.end, null
            );

            String reason = null;
            if (isOccupied) {
                reason = "Conflicts with an existing reservation or active waitlist hold.";
                conflict++;
            } else {
                valid++;
            }

            response.getOccurrences().add(new RecurringBookingPreviewResponse.OccurrenceDto(
                    seq++, occ.start, occ.end, !isOccupied, reason
            ));
        }

        response.setTotalOccurrences(rawOccurrences.size());
        response.setValidCount(valid);
        response.setConflictCount(conflict);
        response.setAllAvailable(conflict == 0 && !rawOccurrences.isEmpty());

        return response;
    }

    /**
     * Create Recurring Booking Series (Task T4):
     * Atomically validates all occurrences; creates series with shared recurrenceId.
     */
    @Transactional
    public List<Booking> createRecurring(RecurringBookingRequest request, UserPrincipal caller) {
        validateRequest(request, caller);

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.getEquipmentId()));

        User user = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<OccurrenceWindow> rawOccurrences = generateOccurrences(
                request.getFrequency(),
                request.getInterval(),
                request.getWeekdays(),
                request.getStartTime(),
                request.getEndTime(),
                request.getUntilDate(),
                maxRecurrenceOccurrences
        );

        if (rawOccurrences.isEmpty()) {
            throw new BadRequestException("No occurrences could be generated with the specified recurrence rule.");
        }

        // Verify that NO occurrences conflict
        List<String> conflicts = new ArrayList<>();
        for (OccurrenceWindow occ : rawOccurrences) {
            if (sharedOccupancyService.isTimeSlotOccupied(equipment.getId(), occ.start, occ.end, null)) {
                conflicts.add(occ.start + " to " + occ.end);
            }
        }

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Scheduling conflict in recurring series: " + conflicts.size() +
                    " occurrence(s) collide with existing bookings. First conflict at: " + conflicts.get(0));
        }

        String recurrenceId = UUID.randomUUID().toString();
        BookingStatus status = equipment.isRequiresApproval() ? BookingStatus.PENDING_APPROVAL : BookingStatus.CONFIRMED;

        List<Booking> savedBookings = new ArrayList<>();
        for (OccurrenceWindow occ : rawOccurrences) {
            Booking b = new Booking();
            b.setEquipment(equipment);
            b.setUser(user);
            b.setStartTime(occ.start);
            b.setEndTime(occ.end);
            b.setPurpose(request.getPurpose().trim());
            b.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
            b.setStatus(status);
            b.setRecurrenceId(recurrenceId);
            savedBookings.add(b);
        }

        List<Booking> persisted = bookingRepository.saveAll(savedBookings);
        logger.info("Created recurring series {} with {} occurrences for User {} on Equipment '{}'",
                recurrenceId, persisted.size(), user.getEmail(), equipment.getName());

        return persisted;
    }

    /**
     * Retrieve all occurrences of a recurring series.
     */
    @Transactional(readOnly = true)
    public List<Booking> getSeriesBookings(String recurrenceId) {
        return bookingRepository.findByRecurrenceIdOrderByStartTimeAsc(recurrenceId);
    }

    /**
     * Cancel future unstarted occurrences in a recurring series.
     */
    @Transactional
    public int cancelSeries(String recurrenceId, String reason, UserPrincipal caller) {
        List<Booking> series = bookingRepository.findByRecurrenceIdOrderByStartTimeAsc(recurrenceId);
        if (series.isEmpty()) {
            throw new ResourceNotFoundException("No series found with recurrence ID: " + recurrenceId);
        }

        Booking first = series.get(0);
        boolean isOwner = first.getUser().getId().equals(caller.getId());
        boolean isManager = caller.getRole() == Role.LAB_MANAGER ||
                            caller.getRole() == Role.INSTITUTION_ADMIN ||
                            caller.getRole() == Role.SYSTEM_ADMIN;

        if (!isOwner && !isManager) {
            throw new UnauthorizedException("You do not have permission to cancel this series.");
        }

        if (!isOwner && isManager && (reason == null || reason.trim().isEmpty())) {
            throw new BadRequestException("Cancellation reason is mandatory when a manager cancels a series.");
        }

        LocalDateTime now = LocalDateTime.now();
        int cancelledCount = 0;

        for (Booking b : series) {
            if (b.getStartTime().isAfter(now) &&
                (b.getStatus() == BookingStatus.PENDING_APPROVAL || b.getStatus() == BookingStatus.CONFIRMED)) {
                b.setStatus(BookingStatus.CANCELLED);
                b.setCancellationReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by user");
                bookingRepository.save(b);
                cancelledCount++;

                eventPublisher.publishEvent(new ReleasedTimeEvent(
                        b.getEquipment().getId(), b.getStartTime(), b.getEndTime(), "SERIES_CANCELLATION"
                ));
            }
        }

        logger.info("Cancelled {} future occurrences in series {} by {}", cancelledCount, recurrenceId, caller.getUsername());
        return cancelledCount;
    }

    /**
     * Approve pending occurrences of a recurring series.
     */
    @Transactional
    public int approveSeries(String recurrenceId, UserPrincipal caller) {
        verifyApproverRole(caller);

        List<Booking> series = bookingRepository.findByRecurrenceIdAndStatusIn(
                recurrenceId, List.of(BookingStatus.PENDING_APPROVAL)
        );

        User manager = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        LocalDateTime now = LocalDateTime.now();
        int approvedCount = 0;

        for (Booking b : series) {
            if (b.getStartTime().isAfter(now)) {
                b.setStatus(BookingStatus.CONFIRMED);
                b.setApprovedBy(manager);
                bookingRepository.save(b);
                approvedCount++;
            }
        }

        logger.info("Approved {} occurrences in series {} by manager {}", approvedCount, recurrenceId, caller.getUsername());
        return approvedCount;
    }

    /**
     * Reject pending occurrences of a recurring series.
     */
    @Transactional
    public int rejectSeries(String recurrenceId, String reason, UserPrincipal caller) {
        verifyApproverRole(caller);

        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is mandatory.");
        }

        List<Booking> series = bookingRepository.findByRecurrenceIdAndStatusIn(
                recurrenceId, List.of(BookingStatus.PENDING_APPROVAL)
        );

        User manager = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        LocalDateTime now = LocalDateTime.now();
        int rejectedCount = 0;

        for (Booking b : series) {
            if (b.getStartTime().isAfter(now)) {
                b.setStatus(BookingStatus.CANCELLED);
                b.setApprovedBy(manager);
                b.setRejectionReason(reason.trim());
                bookingRepository.save(b);
                rejectedCount++;

                eventPublisher.publishEvent(new ReleasedTimeEvent(
                        b.getEquipment().getId(), b.getStartTime(), b.getEndTime(), "SERIES_REJECTION"
                ));
            }
        }

        logger.info("Rejected {} occurrences in series {} by manager {}", rejectedCount, recurrenceId, caller.getUsername());
        return rejectedCount;
    }

    // --- Pure Date Generator ---

    public static class OccurrenceWindow {
        public LocalDateTime start;
        public LocalDateTime end;
        public OccurrenceWindow(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    public static List<OccurrenceWindow> generateOccurrences(
            String frequency, int interval, List<Integer> weekdays,
            LocalDateTime firstStart, LocalDateTime firstEnd,
            LocalDate untilDate, int maxLimit) {

        List<OccurrenceWindow> occurrences = new ArrayList<>();
        LocalTime startTimeOfDay = firstStart.toLocalTime();
        LocalTime endTimeOfDay = firstEnd.toLocalTime();
        LocalDate startDate = firstStart.toLocalDate();

        if (interval < 1) interval = 1;

        if ("DAILY".equalsIgnoreCase(frequency)) {
            LocalDate cur = startDate;
            while (!cur.isAfter(untilDate) && occurrences.size() < maxLimit) {
                occurrences.add(new OccurrenceWindow(
                        LocalDateTime.of(cur, startTimeOfDay),
                        LocalDateTime.of(cur, endTimeOfDay)
                ));
                cur = cur.plusDays(interval);
            }
        } else if ("WEEKLY".equalsIgnoreCase(frequency)) {
            Set<Integer> selectedDays = (weekdays != null && !weekdays.isEmpty())
                    ? new HashSet<>(weekdays)
                    : Set.of(startDate.getDayOfWeek().getValue());

            LocalDate cur = startDate;
            // Align to current week's Monday
            LocalDate weekMonday = cur.with(java.time.DayOfWeek.MONDAY);

            while (!weekMonday.isAfter(untilDate) && occurrences.size() < maxLimit) {
                for (int dow = 1; dow <= 7; dow++) {
                    if (selectedDays.contains(dow)) {
                        LocalDate targetDate = weekMonday.plusDays(dow - 1);
                        if (!targetDate.isBefore(startDate) && !targetDate.isAfter(untilDate)) {
                            occurrences.add(new OccurrenceWindow(
                                    LocalDateTime.of(targetDate, startTimeOfDay),
                                    LocalDateTime.of(targetDate, endTimeOfDay)
                            ));
                            if (occurrences.size() >= maxLimit) break;
                        }
                    }
                }
                weekMonday = weekMonday.plusWeeks(interval);
            }
        }

        return occurrences;
    }

    private void validateRequest(RecurringBookingRequest request, UserPrincipal caller) {
        if (caller.getRole() != Role.RESEARCHER_STUDENT && caller.getRole() != Role.DEPARTMENT_HEAD) {
            throw new UnauthorizedException("Only Researchers/Students and Department Heads are permitted to create bookings.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now)) {
            throw new BadRequestException("First booking start time must be in the future.");
        }
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time.");
        }

        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        if (duration.toMinutes() < 15) {
            throw new BadRequestException("Minimum booking duration is 15 minutes.");
        }
        if (duration.toHours() > 48) {
            throw new BadRequestException("Maximum single booking duration is 48 hours.");
        }

        if (request.getUntilDate().isBefore(request.getStartTime().toLocalDate())) {
            throw new BadRequestException("Until date cannot be before the first booking date.");
        }

        if (!"DAILY".equalsIgnoreCase(request.getFrequency()) && !"WEEKLY".equalsIgnoreCase(request.getFrequency())) {
            throw new BadRequestException("Frequency must be DAILY or WEEKLY.");
        }
    }

    private void verifyApproverRole(UserPrincipal caller) {
        if (caller.getRole() != Role.LAB_MANAGER &&
            caller.getRole() != Role.INSTITUTION_ADMIN &&
            caller.getRole() != Role.SYSTEM_ADMIN) {
            throw new UnauthorizedException("Only Lab Managers, Institution Administrators, and System Administrators can approve series.");
        }
    }
}
