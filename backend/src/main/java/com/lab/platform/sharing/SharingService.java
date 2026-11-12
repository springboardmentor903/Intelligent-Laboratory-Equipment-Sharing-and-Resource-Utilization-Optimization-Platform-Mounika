package com.lab.platform.sharing;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.booking.BookingStatus;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ConflictException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.analytics.OperatingHoursService;
import com.lab.platform.usage.UsageLog;
import com.lab.platform.usage.UsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SharingService {

    private static final Logger logger = LoggerFactory.getLogger(SharingService.class);

    private final SharingListingRepository listingRepository;
    private final SharingAgreementRepository agreementRepository;
    private final AccessGrantRepository grantRepository;
    private final AccessRequestRepository requestRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;
    private final OperatingHoursService operatingHoursService;

    public SharingService(
            SharingListingRepository listingRepository,
            SharingAgreementRepository agreementRepository,
            AccessGrantRepository grantRepository,
            AccessRequestRepository requestRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository,
            OperatingHoursService operatingHoursService) {
        this.listingRepository = listingRepository;
        this.agreementRepository = agreementRepository;
        this.grantRepository = grantRepository;
        this.requestRepository = requestRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
        this.operatingHoursService = operatingHoursService;
    }

    // =========================================================================
    // 1. Sharing Listings (Publishing & Discovery)
    // =========================================================================

    @Transactional
    public SharingListing createOrUpdateListing(SharingListingDto dto, UserPrincipal caller) {
        User user = getUser(caller.getId());
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + dto.getEquipmentId()));

        if (!equipment.isSharableInterInstitution()) {
            throw new BadRequestException("Equipment '" + equipment.getName() + "' is not marked as sharable for inter-institution use.");
        }

        // Authorization: SYSTEM_ADMIN or INSTITUTION_ADMIN of the equipment's institution
        boolean isSysAdmin = caller.getRole() == Role.SYSTEM_ADMIN;
        Long equipmentInstId = equipment.getDepartment() != null && equipment.getDepartment().getInstitution() != null
                ? equipment.getDepartment().getInstitution().getId() : null;
        Long userInstId = user.getDepartment() != null && user.getDepartment().getInstitution() != null
                ? user.getDepartment().getInstitution().getId() : null;

        boolean isHostAdmin = caller.getRole() == Role.INSTITUTION_ADMIN && equipmentInstId != null && equipmentInstId.equals(userInstId);
        if (!isSysAdmin && !isHostAdmin) {
            throw new UnauthorizedException("Only the Institution Administrator of the equipment's host institution or System Admin can manage sharing listings.");
        }

        SharingListing listing = listingRepository.findByEquipmentId(equipment.getId())
                .orElse(new SharingListing());

        listing.setEquipment(equipment);
        listing.setFeePerHour(dto.getFeePerHour());
        listing.setExternalSharePercentage(dto.getExternalSharePercentage());
        listing.setMinNoticeHours(dto.getMinNoticeHours());
        listing.setMaxBookingHours(dto.getMaxBookingHours());
        listing.setTermsAndConditions(dto.getTermsAndConditions());
        listing.setActive(dto.isActive());

        SharingListing saved = listingRepository.save(listing);
        logger.info("Sharing listing published/updated for Equipment '{}' (Listing ID: {}) by user {}",
                equipment.getName(), saved.getId(), caller.getUsername());
        return saved;
    }

    public Optional<SharingListing> getListingByEquipmentId(Long equipmentId) {
        return listingRepository.findByEquipmentId(equipmentId);
    }

    public List<SharingListing> searchListings(Long institutionId, String category, String keyword) {
        return listingRepository.searchListings(institutionId, category, keyword);
    }

    @Transactional
    public SharingListing toggleListingActive(Long listingId, boolean active, UserPrincipal caller) {
        SharingListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Sharing listing not found with id: " + listingId));

        validateListingAdminAccess(listing, caller);
        listing.setActive(active);
        return listingRepository.save(listing);
    }

    // =========================================================================
    // 2. Access Requests (2-Step Approval Workflow)
    // =========================================================================

    @Transactional
    public AccessRequest submitAccessRequest(AccessRequestCreateDto dto, UserPrincipal caller) {
        User requester = getUser(caller.getId());
        if (caller.getRole() != Role.RESEARCHER_STUDENT && caller.getRole() != Role.DEPARTMENT_HEAD) {
            throw new BadRequestException("Only researchers and department heads can submit inter-institution equipment access requests.");
        }

        SharingListing listing = listingRepository.findById(dto.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Sharing listing not found with id: " + dto.getListingId()));

        if (!listing.isActive()) {
            throw new BadRequestException("This equipment sharing listing is currently inactive.");
        }

        Institution requesterInstitution = requester.getDepartment() != null ? requester.getDepartment().getInstitution() : null;
        if (requesterInstitution == null) {
            throw new BadRequestException("Your account is not assigned to an active institution.");
        }

        Institution hostInstitution = listing.getEquipment().getDepartment() != null
                ? listing.getEquipment().getDepartment().getInstitution() : null;

        if (hostInstitution != null && hostInstitution.getId().equals(requesterInstitution.getId())) {
            throw new BadRequestException("Equipment belongs to your own institution. Please use standard internal booking reservations.");
        }

        // Prevent duplicate active access grants
        grantRepository.findByUserIdAndEquipmentIdAndActiveTrue(requester.getId(), listing.getEquipment().getId())
                .ifPresent(grant -> {
                    if (grant.isValidForDate(LocalDate.now())) {
                        throw new ConflictException("You already have an active access grant for this equipment valid until " + grant.getValidTo());
                    }
                });

        AccessRequest request = new AccessRequest(
                listing,
                requester,
                requesterInstitution,
                dto.getPurpose(),
                dto.getEstimatedUsageHours()
        );

        AccessRequest saved = requestRepository.save(request);
        logger.info("Access request #{} submitted by user {} for equipment '{}' (host: {})",
                saved.getId(), caller.getUsername(), listing.getEquipment().getName(),
                hostInstitution != null ? hostInstitution.getName() : "Unknown");
        return saved;
    }

    public List<AccessRequest> getMyRequests(UserPrincipal caller) {
        return requestRepository.findByRequesterIdOrderByRequestedAtDesc(caller.getId());
    }

    public List<AccessRequest> getOutgoingRequests(UserPrincipal caller) {
        User user = getUser(caller.getId());
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            return requestRepository.findAll();
        }
        if (caller.getRole() != Role.INSTITUTION_ADMIN) {
            throw new UnauthorizedException("Only Institution Administrators can review outgoing access requests.");
        }
        Long instId = getInstitutionId(user);
        return requestRepository.findByRequesterInstitutionIdOrderByRequestedAtDesc(instId);
    }

    public List<AccessRequest> getIncomingRequests(UserPrincipal caller) {
        User user = getUser(caller.getId());
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            return requestRepository.findAll();
        }
        if (caller.getRole() != Role.INSTITUTION_ADMIN) {
            throw new UnauthorizedException("Only Institution Administrators can review incoming access requests.");
        }
        Long hostInstId = getInstitutionId(user);
        return requestRepository.findByHostInstitutionId(hostInstId);
    }

    @Transactional
    public AccessRequest forwardRequestByHome(Long requestId, String notes, UserPrincipal caller) {
        User homeAdmin = getUser(caller.getId());
        AccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found with id: " + requestId));

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            if (caller.getRole() != Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Only Institution Administrators can endorse and forward outgoing requests.");
            }
            Long homeInstId = getInstitutionId(homeAdmin);
            if (!homeInstId.equals(request.getRequesterInstitution().getId())) {
                throw new UnauthorizedException("You can only endorse requests originating from your own institution.");
            }
        }

        if (request.getStatus() != AccessRequestStatus.PENDING_HOME_APPROVAL) {
            throw new BadRequestException("Request cannot be forwarded. Current status is " + request.getStatus());
        }

        request.setStatus(AccessRequestStatus.FORWARDED_TO_HOST);
        request.setHomeAdmin(homeAdmin);
        request.setHomeDecisionNotes(notes != null ? notes : "Forwarded with home institution endorsement.");
        request.setHomeDecisionAt(LocalDateTime.now());

        AccessRequest saved = requestRepository.save(request);
        logger.info("Access request #{} endorsed & forwarded to host institution by Home Admin {}",
                saved.getId(), caller.getUsername());
        return saved;
    }

    @Transactional
    public AccessRequest rejectRequestByHome(Long requestId, String reason, UserPrincipal caller) {
        User homeAdmin = getUser(caller.getId());
        AccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found with id: " + requestId));

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            if (caller.getRole() != Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Only Institution Administrators can reject outgoing requests.");
            }
            Long homeInstId = getInstitutionId(homeAdmin);
            if (!homeInstId.equals(request.getRequesterInstitution().getId())) {
                throw new UnauthorizedException("You can only review requests from your own institution.");
            }
        }

        if (request.getStatus() != AccessRequestStatus.PENDING_HOME_APPROVAL) {
            throw new BadRequestException("Only PENDING_HOME_APPROVAL requests can be rejected by home admin.");
        }

        request.setStatus(AccessRequestStatus.REJECTED_BY_HOME);
        request.setHomeAdmin(homeAdmin);
        request.setHomeDecisionNotes(reason != null ? reason : "Declined by home institution administrator.");
        request.setHomeDecisionAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Transactional
    public AccessRequest approveRequestByHost(Long requestId, LocalDate validFrom, LocalDate validTo, String notes, UserPrincipal caller) {
        User hostAdmin = getUser(caller.getId());
        AccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found with id: " + requestId));

        Institution hostInst = request.getListing().getEquipment().getDepartment().getInstitution();

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            if (caller.getRole() != Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Only host Institution Administrators can approve incoming access requests.");
            }
            Long hostInstId = getInstitutionId(hostAdmin);
            if (!hostInstId.equals(hostInst.getId())) {
                throw new UnauthorizedException("You can only approve requests targeting equipment hosted by your institution.");
            }
        }

        if (request.getStatus() != AccessRequestStatus.FORWARDED_TO_HOST) {
            throw new BadRequestException("Request must be endorsed by the home institution before host approval. Current status: " + request.getStatus());
        }

        LocalDate from = (validFrom != null) ? validFrom : LocalDate.now();
        LocalDate to = (validTo != null) ? validTo : LocalDate.now().plusMonths(6);
        if (to.isBefore(from)) {
            throw new BadRequestException("Access validity end date cannot precede start date.");
        }

        Institution requesterInst = request.getRequesterInstitution();

        // Find or create active SharingAgreement between Host (provider) and Requester (consumer)
        SharingAgreement agreement = agreementRepository
                .findByProviderInstitutionIdAndConsumerInstitutionIdAndStatus(hostInst.getId(), requesterInst.getId(), SharingAgreementStatus.ACTIVE)
                .orElseGet(() -> {
                    String agrNumber = "AGR-" + hostInst.getId() + "-" + requesterInst.getId() + "-" + System.currentTimeMillis();
                    SharingAgreement newAgr = new SharingAgreement(
                            agrNumber,
                            hostInst,
                            requesterInst,
                            LocalDate.now(),
                            LocalDate.now().plusYears(1),
                            request.getListing().getFeePerHour(),
                            "Standard Inter-Institution Resource Sharing Terms"
                    );
                    return agreementRepository.save(newAgr);
                });

        // Create AccessGrant
        AccessGrant grant = new AccessGrant(
                agreement,
                request.getRequester(),
                request.getListing().getEquipment(),
                from,
                to
        );
        AccessGrant savedGrant = grantRepository.save(grant);

        // Update Request
        request.setStatus(AccessRequestStatus.APPROVED);
        request.setHostAdmin(hostAdmin);
        request.setHostDecisionNotes(notes != null ? notes : "Approved by host institution administrator.");
        request.setHostDecisionAt(LocalDateTime.now());
        request.setGrantedValidFrom(from);
        request.setGrantedValidTo(to);
        request.setGeneratedGrant(savedGrant);

        AccessRequest savedRequest = requestRepository.save(request);
        logger.info("Access request #{} APPROVED by host admin {}. AccessGrant #{} issued for user {} on equipment '{}'",
                savedRequest.getId(), caller.getUsername(), savedGrant.getId(),
                request.getRequester().getEmail(), request.getListing().getEquipment().getName());
        return savedRequest;
    }

    @Transactional
    public AccessRequest rejectRequestByHost(Long requestId, String reason, UserPrincipal caller) {
        User hostAdmin = getUser(caller.getId());
        AccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found with id: " + requestId));

        Institution hostInst = request.getListing().getEquipment().getDepartment().getInstitution();

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            if (caller.getRole() != Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Only host Institution Administrators can reject incoming access requests.");
            }
            Long hostInstId = getInstitutionId(hostAdmin);
            if (!hostInstId.equals(hostInst.getId())) {
                throw new UnauthorizedException("You can only review requests targeting equipment hosted by your institution.");
            }
        }

        if (request.getStatus() != AccessRequestStatus.FORWARDED_TO_HOST) {
            throw new BadRequestException("Only FORWARDED_TO_HOST requests can be rejected by host admin.");
        }

        request.setStatus(AccessRequestStatus.REJECTED_BY_HOST);
        request.setHostAdmin(hostAdmin);
        request.setHostDecisionNotes(reason != null ? reason : "Declined by equipment host institution administrator.");
        request.setHostDecisionAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Transactional
    public AccessRequest withdrawRequest(Long requestId, UserPrincipal caller) {
        AccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found with id: " + requestId));

        if (!request.getRequester().getId().equals(caller.getId())) {
            throw new UnauthorizedException("Only the requesting researcher can withdraw this access application.");
        }

        if (request.getStatus() != AccessRequestStatus.PENDING_HOME_APPROVAL && request.getStatus() != AccessRequestStatus.FORWARDED_TO_HOST) {
            throw new BadRequestException("Cannot withdraw request with status: " + request.getStatus());
        }

        request.setStatus(AccessRequestStatus.WITHDRAWN);
        return requestRepository.save(request);
    }

    // =========================================================================
    // 3. Agreements & Grants Management
    // =========================================================================

    public List<SharingAgreement> getAgreementsForInstitution(UserPrincipal caller) {
        User user = getUser(caller.getId());
        if (caller.getRole() == Role.SYSTEM_ADMIN) {
            return agreementRepository.findAll();
        }
        Long instId = getInstitutionId(user);
        return agreementRepository.findByInstitutionId(instId);
    }

    public List<AccessGrant> getMyActiveGrants(UserPrincipal caller) {
        List<AccessGrant> grants = grantRepository.findByUserIdAndActiveTrue(caller.getId());
        LocalDate today = LocalDate.now();
        return grants.stream().filter(g -> g.isValidForDate(today)).collect(Collectors.toList());
    }

    @Transactional
    public SharingAgreement revokeAgreement(Long agreementId, String reason, UserPrincipal caller) {
        User user = getUser(caller.getId());
        SharingAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Sharing agreement not found with id: " + agreementId));

        if (caller.getRole() != Role.SYSTEM_ADMIN) {
            Long userInstId = getInstitutionId(user);
            boolean isParty = agreement.getProviderInstitution().getId().equals(userInstId)
                    || agreement.getConsumerInstitution().getId().equals(userInstId);
            if (!isParty || caller.getRole() != Role.INSTITUTION_ADMIN) {
                throw new UnauthorizedException("Only Institution Administrators of participant institutions can revoke agreements.");
            }
        }

        agreement.setStatus(SharingAgreementStatus.REVOKED);
        agreement.setRevocationReason(reason != null ? reason : "Revoked by institutional administrator.");
        SharingAgreement saved = agreementRepository.save(agreement);

        // Deactivate all associated grants
        List<AccessGrant> associatedGrants = grantRepository.findByAgreementId(agreementId);
        for (AccessGrant g : associatedGrants) {
            g.setActive(false);
            grantRepository.save(g);
        }

        logger.info("Sharing agreement #{} revoked by user {}. Deactivated {} associated access grants.",
                agreementId, caller.getUsername(), associatedGrants.size());
        return saved;
    }

    // =========================================================================
    // 4. External Booking Validation & Capacity Enforcement (Task T5)
    // =========================================================================

    /**
     * Validates an inter-institution booking attempt against active Access Grants,
     * minimum notice hours, maximum booking duration, and the weekly external sharing capacity limit.
     */
    public ExternalBookingValidationResult validateAndProcessExternalBooking(
            Equipment equipment, User user, LocalDateTime startTime, LocalDateTime endTime) {

        // 1. Verify active AccessGrant
        AccessGrant grant = grantRepository.findByUserIdAndEquipmentIdAndActiveTrue(user.getId(), equipment.getId())
                .filter(g -> g.isValidForDate(startTime.toLocalDate()))
                .orElseThrow(() -> new UnauthorizedException(
                        "You do not have an active inter-institution Access Grant to book Equipment '"
                                + equipment.getName() + "'. Please submit an Access Request through the Shared Resources portal."));

        SharingAgreement agreement = grant.getAgreement();
        if (agreement == null || agreement.getStatus() != SharingAgreementStatus.ACTIVE) {
            throw new UnauthorizedException("The inter-institution sharing agreement for this equipment is currently inactive or revoked.");
        }

        // 2. Verify active SharingListing
        SharingListing listing = listingRepository.findByEquipmentId(equipment.getId())
                .filter(SharingListing::isActive)
                .orElseThrow(() -> new BadRequestException("Equipment sharing listing is currently not active for external bookings."));

        // 3. Verify Minimum Advance Notice Hours
        LocalDateTime minNoticeBoundary = LocalDateTime.now().plusHours(listing.getMinNoticeHours());
        if (startTime.isBefore(minNoticeBoundary)) {
            throw new BadRequestException("Inter-institution bookings require a minimum advance notice of "
                    + listing.getMinNoticeHours() + " hours (earliest allowed: " + minNoticeBoundary + ").");
        }

        // 4. Verify Maximum Booking Duration
        long requestedDurationMinutes = Duration.between(startTime, endTime).toMinutes();
        long maxDurationMinutes = (long) listing.getMaxBookingHours() * 60;
        if (requestedDurationMinutes > maxDurationMinutes) {
            throw new BadRequestException("Requested booking duration (" + (requestedDurationMinutes / 60.0)
                    + " hours) exceeds the maximum allowed external session length of " + listing.getMaxBookingHours() + " hours.");
        }

        // 5. Verify Weekly External Share Percentage Capacity
        // Calculate operating capacity for the target week (Monday to Sunday)
        LocalDate bookingDate = startTime.toLocalDate();
        LocalDate weekMonday = bookingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSunday = bookingDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime weekStart = weekMonday.atStartOfDay();
        LocalDateTime weekEnd = weekSunday.atTime(23, 59, 59);

        long weeklyCapacityMinutes = operatingHoursService.calculateWeeklyCapacityMinutes(equipment.getId(), bookingDate);
        long maxAllowedExternalMinutes = (weeklyCapacityMinutes * listing.getExternalSharePercentage()) / 100;

        // Query confirmed external bookings on this equipment during this week
        List<Booking> weekBookings = bookingRepository.findByEquipmentIdAndWindow(equipment.getId(), weekStart, weekEnd);
        long currentExternalMinutes = weekBookings.stream()
                .filter(b -> b.getSharingAgreementId() != null)
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.IN_USE || b.getStatus() == BookingStatus.COMPLETED)
                .mapToLong(b -> Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                .sum();

        if (currentExternalMinutes + requestedDurationMinutes > maxAllowedExternalMinutes) {
            throw new ConflictException("Weekly external sharing limit of " + listing.getExternalSharePercentage()
                    + "% (" + (maxAllowedExternalMinutes / 60) + " hours total capacity) would be exceeded for this week. "
                    + "Current external bookings: " + (currentExternalMinutes / 60.0) + " hrs; requested: "
                    + (requestedDurationMinutes / 60.0) + " hrs.");
        }

        // 6. Calculate Estimated Usage Fee
        BigDecimal feePerHour = (listing.getFeePerHour() != null) ? listing.getFeePerHour() : BigDecimal.ZERO;
        BigDecimal estimatedFee = feePerHour.multiply(BigDecimal.valueOf(requestedDurationMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return new ExternalBookingValidationResult(agreement.getId(), estimatedFee);
    }

    // =========================================================================
    // 5. Shared Schedule View API with Privacy Masking (Task T5)
    // =========================================================================

    public List<SharedScheduleSlotDto> getSharedEquipmentSchedule(Long equipmentId, UserPrincipal caller) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + equipmentId));

        boolean canSeeInternalDetails = false;
        if (caller != null) {
            User user = getUser(caller.getId());
            Long userInstId = getInstitutionId(user);
            Long eqInstId = (equipment.getDepartment() != null && equipment.getDepartment().getInstitution() != null)
                    ? equipment.getDepartment().getInstitution().getId() : null;

            if (caller.getRole() == Role.SYSTEM_ADMIN || (eqInstId != null && eqInstId.equals(userInstId))) {
                canSeeInternalDetails = true;
            }
        }

        LocalDateTime horizonStart = LocalDateTime.now().minusDays(14);
        LocalDateTime horizonEnd = LocalDateTime.now().plusDays(45);

        List<Booking> bookings = bookingRepository.findByEquipmentIdAndWindow(equipmentId, horizonStart, horizonEnd);
        List<SharedScheduleSlotDto> slots = new ArrayList<>();

        for (Booking b : bookings) {
            boolean isExternal = (b.getSharingAgreementId() != null);
            String title;
            if (canSeeInternalDetails) {
                title = b.getUser().getFullName() + (isExternal ? " (External Partner)" : " (Internal)");
            } else {
                title = isExternal ? "Reserved (Partner Institution)" : "Reserved (Internal Research)";
            }
            slots.add(new SharedScheduleSlotDto(b.getId(), b.getStartTime(), b.getEndTime(), title, isExternal));
        }

        return slots;
    }

    // =========================================================================
    // 6. Usage Fees Breakdown API (Task T5)
    // =========================================================================

    public List<ItemizedFeeItemDto> getItemizedFeesForAgreement(Long agreementId, UserPrincipal caller) {
        SharingAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Sharing agreement not found with id: " + agreementId));

        validateAgreementPartyAccess(agreement, caller);

        // Find all completed bookings linked to this agreement
        List<UsageLog> sharedLogs = usageLogRepository.findAll().stream()
                .filter(l -> l.getBooking() != null && agreementId.equals(l.getBooking().getSharingAgreementId()))
                .filter(l -> l.getActualEndTime() != null)
                .collect(Collectors.toList());

        List<ItemizedFeeItemDto> items = new ArrayList<>();
        for (UsageLog log : sharedLogs) {
            ItemizedFeeItemDto item = new ItemizedFeeItemDto();
            item.setBookingId(log.getBooking().getId());
            item.setUsageLogId(log.getId());
            item.setEquipmentName(log.getEquipment().getName());
            item.setResearcherName(log.getUser().getFullName());
            item.setConsumerInstitutionName(agreement.getConsumerInstitution().getName());
            item.setSessionStartTime(log.getActualStartTime());
            item.setSessionEndTime(log.getActualEndTime());

            long duration = log.calculateDurationMinutes(LocalDateTime.now());
            item.setDurationMinutes(duration);
            item.setHourlyRate(agreement.getFeePerHour());

            BigDecimal fee = (log.getFeeCharged() != null)
                    ? log.getFeeCharged()
                    : agreement.getFeePerHour().multiply(BigDecimal.valueOf(duration))
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            item.setFeeCharged(fee);

            items.add(item);
        }

        return items;
    }

    // =========================================================================
    // Helpers & Security Validations
    // =========================================================================

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Long getInstitutionId(User user) {
        if (user.getDepartment() != null && user.getDepartment().getInstitution() != null) {
            return user.getDepartment().getInstitution().getId();
        }
        throw new BadRequestException("User is not associated with an institution.");
    }

    private void validateListingAdminAccess(SharingListing listing, UserPrincipal caller) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return;
        User user = getUser(caller.getId());
        Long userInstId = getInstitutionId(user);
        Long hostInstId = listing.getEquipment().getDepartment().getInstitution().getId();
        if (caller.getRole() != Role.INSTITUTION_ADMIN || !userInstId.equals(hostInstId)) {
            throw new UnauthorizedException("Only the host Institution Administrator can modify this listing.");
        }
    }

    private void validateAgreementPartyAccess(SharingAgreement agreement, UserPrincipal caller) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return;
        User user = getUser(caller.getId());
        Long userInstId = getInstitutionId(user);
        boolean isParty = agreement.getProviderInstitution().getId().equals(userInstId)
                || agreement.getConsumerInstitution().getId().equals(userInstId);
        if (!isParty) {
            throw new UnauthorizedException("Access denied: You are not an administrator of either participant institution.");
        }
    }
}
