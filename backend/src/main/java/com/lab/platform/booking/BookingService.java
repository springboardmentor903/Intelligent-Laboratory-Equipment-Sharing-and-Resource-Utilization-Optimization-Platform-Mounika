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

    public BookingService(BookingRepository bookingRepository,
                          EquipmentRepository equipmentRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
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

        // 5. Construct and persist booking
        Booking booking = new Booking();
        booking.setEquipment(equipment);
        booking.setUser(user);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose().trim());
        booking.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        booking.setStatus(BookingStatus.PENDING_APPROVAL);

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

        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        boolean isPrivileged = currentUser.getRole() == Role.ROLE_LAB_MANAGER ||
                               currentUser.getRole() == Role.ROLE_DEPARTMENT_HEAD ||
                               currentUser.getRole() == Role.ROLE_INSTITUTION_ADMIN ||
                               currentUser.getRole() == Role.ROLE_SYSTEM_ADMIN;

        if (!isOwner && !isPrivileged) {
            throw new UnauthorizedException("You do not have permission to cancel this booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : "Cancelled by user");
        Booking updated = bookingRepository.save(booking);

        logger.info("Booking ID {} CANCELLED by {}", bookingId, currentUser.getEmail());
        return updated;
    }
}
