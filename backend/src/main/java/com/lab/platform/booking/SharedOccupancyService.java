package com.lab.platform.booking;

import com.lab.platform.waitlist.Waitlist;
import com.lab.platform.waitlist.WaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SharedOccupancyService {

    private final BookingRepository bookingRepository;
    private final WaitlistRepository waitlistRepository;

    public SharedOccupancyService(BookingRepository bookingRepository,
                                  WaitlistRepository waitlistRepository) {
        this.bookingRepository = bookingRepository;
        this.waitlistRepository = waitlistRepository;
    }

    /**
     * Retrieves all occupied time ranges for an equipment between start and end window,
     * including PENDING_APPROVAL, CONFIRMED, IN_USE bookings, and active OFFERED waitlist holds.
     */
    @Transactional(readOnly = true)
    public List<OccupiedTimeRange> getOccupiedRanges(Long equipmentId, LocalDateTime windowStart, LocalDateTime windowEnd, Long excludeWaitlistHoldId) {
        List<OccupiedTimeRange> ranges = new ArrayList<>();

        // 1. Fetch active bookings
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING_APPROVAL,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_USE
        );
        List<Booking> bookings = bookingRepository.findOverlappingBookingsForEquipment(
                equipmentId, activeStatuses, windowStart, windowEnd
        );

        for (Booking b : bookings) {
            String type = "BOOKING_" + b.getStatus().name();
            ranges.add(new OccupiedTimeRange(b.getStartTime(), b.getEndTime(), type, b.getId()));
        }

        // 2. Fetch active waitlist holds
        LocalDateTime now = LocalDateTime.now();
        List<Waitlist> holds = waitlistRepository.findActiveHolds(
                equipmentId, now, windowStart, windowEnd, excludeWaitlistHoldId
        );

        for (Waitlist w : holds) {
            ranges.add(new OccupiedTimeRange(w.getRequestedStartTime(), w.getRequestedEndTime(), "WAITLIST_HELD", w.getId()));
        }

        return ranges;
    }

    /**
     * Checks if a requested time interval conflicts with any existing booking or active hold.
     */
    @Transactional(readOnly = true)
    public boolean isTimeSlotOccupied(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeWaitlistHoldId) {
        List<OccupiedTimeRange> occupied = getOccupiedRanges(equipmentId, startTime, endTime, excludeWaitlistHoldId);
        for (OccupiedTimeRange r : occupied) {
            if (r.overlaps(startTime, endTime)) {
                return true;
            }
        }
        return false;
    }
}
