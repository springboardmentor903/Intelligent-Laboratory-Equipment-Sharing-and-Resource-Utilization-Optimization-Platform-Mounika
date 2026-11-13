package com.lab.platform.usage;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.booking.BookingStatus;
import com.lab.platform.booking.ReleasedTimeEvent;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentService;
import com.lab.platform.equipment.EquipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private static final Logger logger = LoggerFactory.getLogger(UsageService.class);

    private final UsageLogRepository usageLogRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;
    private final UserRepository userRepository;
    private final EquipmentStatusPublisher statusPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final com.lab.platform.sharing.SharingAgreementRepository agreementRepository;

    public UsageService(UsageLogRepository usageLogRepository,
                        BookingRepository bookingRepository,
                        EquipmentRepository equipmentRepository,
                        EquipmentService equipmentService,
                        UserRepository userRepository,
                        EquipmentStatusPublisher statusPublisher,
                        ApplicationEventPublisher eventPublisher,
                        com.lab.platform.sharing.SharingAgreementRepository agreementRepository) {
        this.usageLogRepository = usageLogRepository;
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentService = equipmentService;
        this.userRepository = userRepository;
        this.statusPublisher = statusPublisher;
        this.eventPublisher = eventPublisher;
        this.agreementRepository = agreementRepository;
    }

    /**
     * Check-in API (Task T1):
     * Allowed only for the booking owner, from 15 mins before scheduled start until scheduled end time.
     * Transitions booking to IN_USE and equipment status to BOOKED.
     */
    @Transactional
    public Booking checkIn(Long bookingId, UserPrincipal caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(caller.getId())) {
            throw new UnauthorizedException("Only the booking owner can check in to this session.");
        }

        if (booking.getStatus() == BookingStatus.IN_USE) {
            throw new BadRequestException("This booking is already checked in and currently in use.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED bookings can be checked in. Current status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowedStart = booking.getStartTime().minusMinutes(15);
        if (now.isBefore(allowedStart)) {
            throw new BadRequestException("Check-in opens 15 minutes before the scheduled booking time (at " + allowedStart + ").");
        }

        if (now.isAfter(booking.getEndTime())) {
            throw new BadRequestException("Cannot check in: The scheduled booking time has already ended.");
        }

        Equipment equipment = booking.getEquipment();

        // 1. Update Booking status to IN_USE
        booking.setStatus(BookingStatus.IN_USE);
        Booking savedBooking = bookingRepository.save(booking);

        // 2. Synchronize equipment status to BOOKED (managed action)
        equipmentService.updateEquipmentStatusInternal(equipment.getId(), EquipmentStatus.BOOKED, true);

        // 3. Create active UsageLog
        UsageLog.UsageType usageType = (booking.getSharingAgreementId() != null)
                ? UsageLog.UsageType.SHARED
                : UsageLog.UsageType.INTERNAL;

        UsageLog usageLog = new UsageLog(equipment, booking.getUser(), booking, now, usageType);
        usageLogRepository.save(usageLog);

        logger.info("Session started: User {} checked in to Equipment '{}' (Booking ID: {})",
                caller.getUsername(), equipment.getName(), bookingId);

        // 4. Broadcast real-time status update via WebSocket
        statusPublisher.publishStatusUpdate(new EquipmentStatusUpdateMessage(
                equipment.getId(),
                equipment.getName(),
                equipment.getAssetTag(),
                EquipmentStatus.BOOKED.name(),
                true,
                now,
                booking.getEndTime(),
                "CHECK_IN"
        ));

        return savedBooking;
    }

    /**
     * Check-out API (Task T1):
     * Allowed only for the booking owner.
     * Transitions booking to COMPLETED, closes UsageLog, returns equipment to AVAILABLE (if still BOOKED).
     */
    @Transactional
    public Booking checkOut(Long bookingId, UserPrincipal caller) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(caller.getId())) {
            throw new UnauthorizedException("Only the booking owner can check out of this session.");
        }

        if (booking.getStatus() != BookingStatus.IN_USE) {
            throw new BadRequestException("Only active IN_USE bookings can be checked out. Current status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        Equipment equipment = booking.getEquipment();

        // 1. Update Booking status to COMPLETED
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        // 2. Close active UsageLog
        UsageLog log = usageLogRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .orElse(null);
        if (log != null && log.getActualEndTime() == null) {
            log.setActualEndTime(now);
            if (booking.getSharingAgreementId() != null) {
                agreementRepository.findById(booking.getSharingAgreementId()).ifPresent(agreement -> {
                    long minutes = log.calculateDurationMinutes(now);
                    java.math.BigDecimal fee = agreement.getFeePerHour()
                            .multiply(java.math.BigDecimal.valueOf(minutes))
                            .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                    log.setFeeCharged(fee);
                });
            }
            usageLogRepository.save(log);
        }

        // 3. Synchronize equipment: return to AVAILABLE only if currently BOOKED
        // Do not overwrite UNDER_MAINTENANCE or OUT_OF_SERVICE
        Equipment currentEq = equipmentRepository.findById(equipment.getId()).orElse(equipment);
        if (currentEq.getStatus() == EquipmentStatus.BOOKED) {
            equipmentService.updateEquipmentStatusInternal(equipment.getId(), EquipmentStatus.AVAILABLE, true);
        }

        // 4. Publish ReleasedTimeEvent if checkout occurred before scheduled end time
        if (now.isBefore(booking.getEndTime())) {
            eventPublisher.publishEvent(new ReleasedTimeEvent(
                    equipment.getId(),
                    now,
                    booking.getEndTime(),
                    "EARLY_CHECK_OUT"
            ));
        }

        logger.info("Session concluded: User {} checked out of Equipment '{}' (Booking ID: {})",
                caller.getUsername(), equipment.getName(), bookingId);

        // 5. Broadcast real-time status update via WebSocket
        statusPublisher.publishStatusUpdate(new EquipmentStatusUpdateMessage(
                equipment.getId(),
                equipment.getName(),
                equipment.getAssetTag(),
                currentEq.getStatus().name(),
                false,
                null,
                null,
                "CHECK_OUT"
        ));

        return savedBooking;
    }

    /**
     * Automatic No-Show Scheduled Job:
     * Runs every 60 seconds.
     * Marks CONFIRMED bookings as NO_SHOW if not checked in within 15 minutes after start time.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void handleAutoNoShow() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Booking> noShowCandidates = bookingRepository.findByStatusAndStartTimeBefore(
                BookingStatus.CONFIRMED, threshold
        );

        for (Booking booking : noShowCandidates) {
            Equipment equipment = booking.getEquipment();
            booking.setStatus(BookingStatus.NO_SHOW);
            booking.setCancellationReason("Auto marked as No-Show: User failed to check in within 15 minutes of scheduled start.");
            bookingRepository.save(booking);

            Equipment currentEq = equipmentRepository.findById(equipment.getId()).orElse(equipment);
            if (currentEq.getStatus() == EquipmentStatus.BOOKED) {
                equipmentService.updateEquipmentStatusInternal(equipment.getId(), EquipmentStatus.AVAILABLE, true);
            }

            // Publish released time so waitlisted users can grab the slot
            eventPublisher.publishEvent(new ReleasedTimeEvent(
                    equipment.getId(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    "NO_SHOW"
            ));

            statusPublisher.publishStatusUpdate(new EquipmentStatusUpdateMessage(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getAssetTag(),
                    EquipmentStatus.AVAILABLE.name(),
                    false,
                    null,
                    null,
                    "NO_SHOW"
            ));

            logger.info("Auto No-Show processed: Booking ID {} for Equipment '{}'", booking.getId(), equipment.getName());
        }
    }

    /**
     * Automatic Session Completion Scheduled Job:
     * Runs every 60 seconds.
     * Closes IN_USE sessions that have exceeded their scheduled booking end time.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void handleAutoCompletion() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> overdueCandidates = bookingRepository.findByStatusAndEndTimeBefore(
                BookingStatus.IN_USE, now
        );

        for (Booking booking : overdueCandidates) {
            Equipment equipment = booking.getEquipment();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            UsageLog log = usageLogRepository.findFirstByBookingIdOrderByIdDesc(booking.getId()).orElse(null);
            if (log != null && log.getActualEndTime() == null) {
                log.setActualEndTime(booking.getEndTime());
                usageLogRepository.save(log);
            }

            Equipment currentEq = equipmentRepository.findById(equipment.getId()).orElse(equipment);
            if (currentEq.getStatus() == EquipmentStatus.BOOKED) {
                equipmentService.updateEquipmentStatusInternal(equipment.getId(), EquipmentStatus.AVAILABLE, true);
            }

            statusPublisher.publishStatusUpdate(new EquipmentStatusUpdateMessage(
                    equipment.getId(),
                    equipment.getName(),
                    equipment.getAssetTag(),
                    EquipmentStatus.AVAILABLE.name(),
                    false,
                    null,
                    null,
                    "AUTO_COMPLETE"
            ));

            logger.info("Auto Completion processed for overdue session: Booking ID {} on Equipment '{}'",
                    booking.getId(), equipment.getName());
        }
    }

    /**
     * Live Equipment Status API:
     * Retrieves operational state, current session with elapsed time, and next booking.
     * Enforces privacy: Bookers' identities are hidden from regular users, visible only to authorized managers.
     */
    @Transactional(readOnly = true)
    public List<LiveEquipmentStatusResponse> getLiveEquipmentStatus(Long institutionId, Long departmentId,
                                                                   EquipmentStatus status, String category,
                                                                   UserPrincipal caller) {
        List<Equipment> equipmentList = equipmentRepository.findByFilter(null, category, status, institutionId, departmentId);

        // Fetch active usage logs (currently active sessions)
        List<UsageLog> activeLogs = usageLogRepository.findByActualEndTimeIsNull();
        Map<Long, UsageLog> activeLogByEquipment = activeLogs.stream()
                .collect(Collectors.toMap(l -> l.getEquipment().getId(), l -> l, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        List<LiveEquipmentStatusResponse> responses = new ArrayList<>();

        for (Equipment eq : equipmentList) {
            LiveEquipmentStatusResponse res = new LiveEquipmentStatusResponse();
            res.setEquipmentId(eq.getId());
            res.setEquipmentName(eq.getName());
            res.setAssetTag(eq.getAssetTag());
            res.setCategory(eq.getCategory());
            res.setLocationRoom(eq.getLocationRoom());
            res.setInstitutionId(eq.getInstitution().getId());
            res.setInstitutionName(eq.getInstitution().getName());
            res.setDepartmentId(eq.getDepartment().getId());
            res.setDepartmentName(eq.getDepartment().getName());
            res.setEquipmentStatus(eq.getStatus());

            UsageLog activeLog = activeLogByEquipment.get(eq.getId());
            boolean isInUse = (activeLog != null);
            res.setInUse(isInUse);

            if (isInUse) {
                res.setSessionStartTime(activeLog.getActualStartTime());
                if (activeLog.getBooking() != null) {
                    res.setScheduledEndTime(activeLog.getBooking().getEndTime());
                }
                long elapsedSeconds = Duration.between(activeLog.getActualStartTime(), now).getSeconds();
                res.setElapsedSeconds(Math.max(0, elapsedSeconds));
                res.setElapsedMinutes(Math.max(0, elapsedSeconds / 60));

                // Privacy check: Only managers of the equipment's institution or System Admin can see user identity
                if (caller != null && canViewBookerDetails(caller, eq)) {
                    res.setCurrentUserId(activeLog.getUser().getId());
                    res.setCurrentUserName(activeLog.getUser().getFullName());
                    res.setCurrentUserEmail(activeLog.getUser().getEmail());
                    if (activeLog.getBooking() != null) {
                        res.setCurrentBookingId(activeLog.getBooking().getId());
                        res.setCurrentBookingPurpose(activeLog.getBooking().getPurpose());
                    }
                }
            }

            // Find next confirmed/pending booking
            Optional<Booking> nextBooking = bookingRepository.findFirstByEquipmentIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
                    eq.getId(),
                    List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_APPROVAL),
                    now
            );

            nextBooking.ifPresent(b -> {
                res.setNextBookingStartTime(b.getStartTime());
                res.setNextBookingEndTime(b.getEndTime());
            });

            responses.add(res);
        }

        return responses;
    }

    private boolean canViewBookerDetails(UserPrincipal caller, Equipment equipment) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            return true;
        }
        User callerUser = userRepository.findById(caller.getId()).orElse(null);
        if (callerUser == null || callerUser.getInstitution() == null) {
            return false;
        }

        boolean sameInstitution = callerUser.getInstitution().getId().equals(equipment.getInstitution().getId());
        if (!sameInstitution) {
            return false;
        }

        return caller.getRole() == Role.LAB_MANAGER || caller.getRole() == Role.INSTITUTION_ADMIN;
    }
}
