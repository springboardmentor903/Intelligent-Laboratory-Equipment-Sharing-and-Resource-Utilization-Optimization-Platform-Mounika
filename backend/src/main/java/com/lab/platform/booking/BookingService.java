package com.lab.platform.booking;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final com.lab.platform.sharing.SharingService sharingService;

    public BookingService(BookingRepository bookingRepository,
                          EquipmentRepository equipmentRepository,
                          UserRepository userRepository,
                          com.lab.platform.sharing.SharingService sharingService) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.sharingService = sharingService;
    }

    @Transactional
    public Booking createBooking(BookingRequest request, String userEmail) {
        // 1. Validate time bounds
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Booking end time must be after the start time.");
        }

        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        if (duration.toMinutes() < 15) {
            throw new BadRequestException("Minimum booking duration is 15 minutes.");
        }
        if (duration.toHours() > 48) {
            throw new BadRequestException("Maximum single booking duration cannot exceed 48 hours.");
        }

        // 2. Fetch User & Equipment
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Booking permission rule: RESEARCHER_STUDENT, DEPARTMENT_HEAD, and SYSTEM_ADMIN can book
        if (user.getRole() != Role.RESEARCHER_STUDENT && user.getRole() != Role.DEPARTMENT_HEAD && user.getRole() != Role.SYSTEM_ADMIN) {
            throw new BadRequestException("Only Researchers/Students, Department Heads, and System Admins are permitted to book equipment.");
        }

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.getEquipmentId()));

        // 3. Check equipment operational status
        if (equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE) {
            throw new BadRequestException("Equipment is currently Out of Service and cannot be booked.");
        }
        if (equipment.getStatus() == EquipmentStatus.UNDER_MAINTENANCE) {
            throw new BadRequestException("Equipment is currently Under Maintenance and cannot be booked.");
        }
        if (equipment.getStatus() == EquipmentStatus.RETIRED) {
            throw new BadRequestException("Equipment is retired from laboratory service.");
        }

        // 4. Strict collision detection
        List<BookingStatus> conflictingStatuses = Arrays.asList(
                BookingStatus.PENDING_APPROVAL,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_USE
        );

        long overlappingCount = bookingRepository.countOverlappingBookings(
                equipment.getId(),
                request.getStartTime(),
                request.getEndTime(),
                conflictingStatuses,
                null
        );

        if (overlappingCount > 0) {
            throw new ConflictException("Scheduling conflict: The requested time slot overlaps with an existing booking or pending reservation for this equipment.");
        }

        // 5. Inter-Institution External Sharing check (Task T5)
        boolean isExternal = equipment.getDepartment() != null
                && equipment.getDepartment().getInstitution() != null
                && user.getDepartment() != null
                && user.getDepartment().getInstitution() != null
                && !equipment.getDepartment().getInstitution().getId().equals(user.getDepartment().getInstitution().getId());

        com.lab.platform.sharing.ExternalBookingValidationResult extResult = null;
        if (isExternal) {
            extResult = sharingService.validateAndProcessExternalBooking(
                    equipment, user, request.getStartTime(), request.getEndTime());
        }

        // 6. Construct and persist booking
        Booking booking = new Booking();
        booking.setEquipment(equipment);
        booking.setUser(user);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose().trim());
        booking.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        booking.setStatus(equipment.isRequiresApproval() ? BookingStatus.PENDING_APPROVAL : BookingStatus.CONFIRMED);

        if (extResult != null) {
            booking.setSharingAgreementId(extResult.getSharingAgreementId());
            booking.setEstimatedFee(extResult.getEstimatedFee());
        }

        Booking saved = bookingRepository.save(booking);
        logger.info("Booking request created: ID {} for Equipment '{}' by User '{}' ({})",
                saved.getId(), equipment.getName(), user.getFullName(), user.getEmail());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<BookingCalendarEvent> getEquipmentCalendarEvents(Long equipmentId) {
        List<Booking> bookings = bookingRepository.findByEquipmentIdOrderByStartTimeAsc(equipmentId);
        return bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(BookingCalendarEvent::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Booking> getPendingBookings(String managerEmail) {
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + managerEmail));

        // If manager has a department, return department pending bookings; otherwise return all pending
        if (manager.getDepartment() != null) {
            return bookingRepository.findByDepartmentAndStatus(manager.getDepartment().getId(), BookingStatus.PENDING_APPROVAL);
        }
        return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING_APPROVAL);
    }

    @Transactional
    public Booking processApproval(Long bookingId, BookingApprovalRequest request, String managerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + managerEmail));

        if (request.isApproved()) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setApprovedBy(manager);
            booking.setRejectionReason(null);
            logger.info("Booking ID {} CONFIRMED by manager {}", bookingId, manager.getEmail());
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setApprovedBy(manager);
            booking.setRejectionReason(request.getReason() != null ? request.getReason() : "Rejected by Lab Manager");
            logger.info("Booking ID {} REJECTED by manager {}: {}", bookingId, manager.getEmail(), booking.getRejectionReason());
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, String reason, String currentUserEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserEmail));

        LocalDateTime now = LocalDateTime.now();
        if (booking.getStartTime().isBefore(now) || booking.getStatus() == BookingStatus.IN_USE ||
                booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW ||
                booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("A booking that has already started or concluded cannot be cancelled.");
        }

        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        boolean isManager = currentUser.getRole() == Role.LAB_MANAGER ||
                            currentUser.getRole() == Role.INSTITUTION_ADMIN ||
                            currentUser.getRole() == Role.SYSTEM_ADMIN;

        if (!isOwner && !isManager) {
            throw new UnauthorizedException("You do not have permission to cancel this booking.");
        }

        if (!isOwner && isManager) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new BadRequestException("A cancellation reason is mandatory when an administrator or manager cancels a booking.");
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by user");
        Booking updated = bookingRepository.save(booking);

        logger.info("Booking ID {} CANCELLED by {}", bookingId, currentUser.getEmail());
        return updated;
    }

    @Transactional
    public Booking shiftToCurrentTime(Long bookingId, String currentUserEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        LocalDateTime now = LocalDateTime.now();
        booking.setStartTime(now.minusMinutes(2));
        booking.setEndTime(now.plusHours(1));
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(booking);
        logger.info("Demo helper: Shifted booking ID {} to current window [{} to {}] for {}",
                bookingId, booking.getStartTime(), booking.getEndTime(), currentUserEmail);
        return updated;
    }
}
