package com.lab.platform.waitlist;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.booking.BookingStatus;
import com.lab.platform.booking.ReleasedTimeEvent;
import com.lab.platform.booking.SharedOccupancyService;
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
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaitlistService {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistService.class);

    private final WaitlistRepository waitlistRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SharedOccupancyService sharedOccupancyService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.waitlist.offer-duration-minutes:60}")
    private long offerDurationMinutes;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           EquipmentRepository equipmentRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           SharedOccupancyService sharedOccupancyService,
                           ApplicationEventPublisher eventPublisher) {
        this.waitlistRepository = waitlistRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.sharedOccupancyService = sharedOccupancyService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Join Waitlist API (Task T4):
     * Allowed only when the requested time slot is unavailable.
     * Enforces FIFO queue, role restrictions, and prevents duplicate active waitlist entries.
     */
    @Transactional
    public WaitlistResponse joinWaitlist(WaitlistJoinRequest request, UserPrincipal caller) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.getEquipmentId()));

        if (equipment.getStatus() == EquipmentStatus.UNDER_MAINTENANCE ||
            equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE ||
            equipment.getStatus() == EquipmentStatus.RETIRED) {
            throw new BadRequestException("Equipment is currently " + equipment.getStatus().getDisplayName() + " and not bookable.");
        }

        // Only Researchers/Students and Department Heads are permitted to book / join waitlist
        if (caller.getRole() != Role.RESEARCHER_STUDENT && caller.getRole() != Role.DEPARTMENT_HEAD) {
            throw new UnauthorizedException("Only Researchers/Students and Department Heads are permitted to join equipment waitlists.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now)) {
            throw new BadRequestException("Requested start time must be in the future.");
        }
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("Requested end time must be after start time.");
        }

        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        if (duration.toMinutes() < 15) {
            throw new BadRequestException("Minimum duration is 15 minutes.");
        }
        if (duration.toHours() > 48) {
            throw new BadRequestException("Maximum duration cannot exceed 48 hours.");
        }

        // Requirement: If slot is available, direct user to book directly
        boolean isOccupied = sharedOccupancyService.isTimeSlotOccupied(equipment.getId(), request.getStartTime(), request.getEndTime(), null);
        if (!isOccupied) {
            throw new BadRequestException("The requested time slot is currently available. Please make a reservation directly instead of joining the waitlist.");
        }

        // Prevent duplicate active waitlist entries for the same user and overlapping window
        long duplicateCount = waitlistRepository.countOverlappingActiveEntries(
                equipment.getId(),
                caller.getId(),
                List.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED),
                request.getStartTime(),
                request.getEndTime()
        );
        if (duplicateCount > 0) {
            throw new ConflictException("You already have an active waitlist entry for this equipment with an overlapping time window.");
        }

        User user = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Waitlist entry = new Waitlist(equipment, user, request.getStartTime(), request.getEndTime());
        Waitlist saved = waitlistRepository.save(entry);

        long queuePos = waitlistRepository.calculateQueuePosition(
                equipment.getId(), saved.getCreatedAt(), saved.getRequestedStartTime(), saved.getRequestedEndTime()
        ) + 1;

        logger.info("User {} joined waitlist for Equipment '{}' (Entry ID: {}, Queue Pos: {})",
                caller.getUsername(), equipment.getName(), saved.getId(), queuePos);

        return WaitlistResponse.fromEntity(saved, queuePos);
    }

    /**
     * Retrieve logged-in user's waitlist entries with status filter and dynamic queue position.
     */
    @Transactional(readOnly = true)
    public Page<WaitlistResponse> getMyWaitlist(WaitlistStatus statusFilter, Pageable pageable, UserPrincipal caller) {
        Page<Waitlist> page;
        if (statusFilter != null) {
            page = waitlistRepository.findByUserIdAndStatus(caller.getId(), statusFilter, pageable);
        } else {
            page = waitlistRepository.findByUserId(caller.getId(), pageable);
        }

        return page.map(w -> {
            long queuePos = 0;
            if (w.getStatus() == WaitlistStatus.WAITING) {
                queuePos = waitlistRepository.calculateQueuePosition(
                        w.getEquipment().getId(), w.getCreatedAt(), w.getRequestedStartTime(), w.getRequestedEndTime()
                ) + 1;
            }
            return WaitlistResponse.fromEntity(w, queuePos);
        });
    }

    /**
     * Accept Offer API:
     * Receives mandatory purpose, creates confirmed or pending booking, marks waitlist ACCEPTED.
     */
    @Transactional
    public Booking acceptOffer(Long waitlistId, WaitlistAcceptRequest request, UserPrincipal caller) {
        Waitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id: " + waitlistId));

        if (!entry.getUser().getId().equals(caller.getId())) {
            throw new UnauthorizedException("You do not have permission to accept this waitlist offer.");
        }

        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BadRequestException("This waitlist entry is not in an OFFERED state. Current status: " + entry.getStatus());
        }

        if (entry.getOfferExpiresAt() != null && entry.getOfferExpiresAt().isBefore(LocalDateTime.now())) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            throw new BadRequestException("This waitlist offer has expired.");
        }

        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            throw new BadRequestException("Booking purpose is mandatory.");
        }

        Equipment equipment = entry.getEquipment();

        // Create booking (ignoring this waitlist entry's hold)
        Booking booking = new Booking();
        booking.setEquipment(equipment);
        booking.setUser(entry.getUser());
        booking.setStartTime(entry.getRequestedStartTime());
        booking.setEndTime(entry.getRequestedEndTime());
        booking.setPurpose(request.getPurpose().trim());
        booking.setNotes(request.getNotes() != null ? request.getNotes().trim() : "Created from waitlist offer");

        BookingStatus initialStatus = equipment.isRequiresApproval() ? BookingStatus.PENDING_APPROVAL : BookingStatus.CONFIRMED;
        booking.setStatus(initialStatus);

        Booking savedBooking = bookingRepository.save(booking);

        entry.setStatus(WaitlistStatus.ACCEPTED);
        waitlistRepository.save(entry);

        logger.info("Waitlist offer accepted: Booking ID {} created for User {} on Equipment '{}'",
                savedBooking.getId(), caller.getUsername(), equipment.getName());

        return savedBooking;
    }

    /**
     * Decline Offer API:
     * Declines offer, transitions status to DECLINED, and publishes released-time event for next queued user.
     */
    @Transactional
    public void declineOffer(Long waitlistId, UserPrincipal caller) {
        Waitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id: " + waitlistId));

        if (!entry.getUser().getId().equals(caller.getId())) {
            throw new UnauthorizedException("You do not have permission to decline this offer.");
        }

        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BadRequestException("Only OFFERED waitlist entries can be declined.");
        }

        entry.setStatus(WaitlistStatus.DECLINED);
        waitlistRepository.save(entry);

        // Publish released time so next in queue gets offer
        eventPublisher.publishEvent(new ReleasedTimeEvent(
                entry.getEquipment().getId(),
                entry.getRequestedStartTime(),
                entry.getRequestedEndTime(),
                "WAITLIST_OFFER_DECLINED"
        ));

        logger.info("Waitlist offer declined by User {} for Equipment '{}'", caller.getUsername(), entry.getEquipment().getName());
    }

    /**
     * Cancel / Leave Waitlist API:
     * User withdraws from waitlist.
     */
    @Transactional
    public void cancelWaitlist(Long waitlistId, UserPrincipal caller) {
        Waitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with id: " + waitlistId));

        if (!entry.getUser().getId().equals(caller.getId())) {
            throw new UnauthorizedException("You do not have permission to cancel this waitlist entry.");
        }

        boolean wasOffered = (entry.getStatus() == WaitlistStatus.OFFERED);
        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.save(entry);

        if (wasOffered) {
            eventPublisher.publishEvent(new ReleasedTimeEvent(
                    entry.getEquipment().getId(),
                    entry.getRequestedStartTime(),
                    entry.getRequestedEndTime(),
                    "WAITLIST_OFFER_CANCELLED"
            ));
        }

        logger.info("Waitlist entry {} CANCELLED by User {}", waitlistId, caller.getUsername());
    }

    /**
     * Event-based released-time detection:
     * Triggered by booking cancellation, rejection, early checkout, or offer expiry.
     * Finds waiting entries in FIFO order and generates non-overlapping offers.
     */
    @EventListener
    @Transactional
    public void handleReleasedTime(ReleasedTimeEvent event) {
        logger.info("Released-time detected for Equipment ID {}: {} to {} (Reason: {})",
                event.getEquipmentId(), event.getReleasedStartTime(), event.getReleasedEndTime(), event.getReason());

        List<Waitlist> waitingList = waitlistRepository.findByEquipmentIdAndStatusOrderByCreatedAtAsc(
                event.getEquipmentId(), WaitlistStatus.WAITING
        );

        LocalDateTime now = LocalDateTime.now();

        for (Waitlist candidate : waitingList) {
            // Must be in the future
            if (candidate.getRequestedStartTime().isBefore(now)) {
                candidate.setStatus(WaitlistStatus.EXPIRED);
                waitlistRepository.save(candidate);
                continue;
            }

            // Must fit within the released window
            boolean fitsWindow = !candidate.getRequestedStartTime().isBefore(event.getReleasedStartTime()) &&
                                 !candidate.getRequestedEndTime().isAfter(event.getReleasedEndTime());

            if (fitsWindow) {
                // Must not overlap with any existing bookings or active holds
                boolean isBlocked = sharedOccupancyService.isTimeSlotOccupied(
                        event.getEquipmentId(),
                        candidate.getRequestedStartTime(),
                        candidate.getRequestedEndTime(),
                        candidate.getId()
                );

                if (!isBlocked) {
                    // Generate offer
                    candidate.setStatus(WaitlistStatus.OFFERED);
                    candidate.setOfferedAt(now);

                    LocalDateTime sixtyMins = now.plusMinutes(offerDurationMinutes);
                    LocalDateTime expiry = sixtyMins.isBefore(candidate.getRequestedStartTime())
                            ? sixtyMins
                            : candidate.getRequestedStartTime();
                    candidate.setOfferExpiresAt(expiry);

                    waitlistRepository.save(candidate);
                    logger.info("Generated waitlist offer: Entry ID {} for User {} on Equipment ID {}. Expires at: {}",
                            candidate.getId(), candidate.getUser().getEmail(), candidate.getEquipment().getId(), expiry);

                    // Once an offer is created, the slot is held (HELD)
                    break;
                }
            }
        }
    }

    /**
     * Scheduled Job: Runs every minute to expire passed offers and waiting requests.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void handleOfferExpiryJob() {
        LocalDateTime now = LocalDateTime.now();

        // Expire passed offers
        List<Waitlist> expiredOffers = waitlistRepository.findByStatusAndOfferExpiresAtBefore(
                WaitlistStatus.OFFERED, now
        );

        for (Waitlist w : expiredOffers) {
            w.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(w);

            logger.info("Waitlist offer expired for Entry ID {}. Slot freed.", w.getId());

            eventPublisher.publishEvent(new ReleasedTimeEvent(
                    w.getEquipment().getId(),
                    w.getRequestedStartTime(),
                    w.getRequestedEndTime(),
                    "WAITLIST_OFFER_EXPIRED"
            ));
        }

        // Expire waiting entries whose requested start time has already passed
        List<Waitlist> expiredWaiting = waitlistRepository.findByStatusAndRequestedStartTimeBefore(
                WaitlistStatus.WAITING, now
        );
        for (Waitlist w : expiredWaiting) {
            w.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(w);
        }
    }
}
