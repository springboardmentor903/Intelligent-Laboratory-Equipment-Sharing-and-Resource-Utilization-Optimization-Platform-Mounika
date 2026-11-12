package com.lab.platform.booking;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final RecurringBookingService recurringBookingService;
    private final BookingSuggestionService bookingSuggestionService;

    public BookingController(BookingService bookingService,
                             RecurringBookingService recurringBookingService,
                             BookingSuggestionService bookingSuggestionService) {
        this.bookingService = bookingService;
        this.recurringBookingService = recurringBookingService;
        this.bookingSuggestionService = bookingSuggestionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN', 'ROLE_RESEARCHER_STUDENT', 'ROLE_DEPARTMENT_HEAD', 'ROLE_SYSTEM_ADMIN', 'RESEARCHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Booking>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Booking booking = bookingService.createBooking(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Booking reservation requested successfully", booking));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        List<Booking> bookings = bookingService.getUserBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/equipment/{equipmentId}/calendar")
    public ResponseEntity<ApiResponse<List<BookingCalendarEvent>>> getEquipmentCalendarEvents(@PathVariable Long equipmentId) {
        List<BookingCalendarEvent> events = bookingService.getEquipmentCalendarEvents(equipmentId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Booking>>> getPendingBookings(@AuthenticationPrincipal UserDetails userDetails) {
        List<Booking> pending = bookingService.getPendingBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Booking>>> getPendingApprovalQueue(
            @RequestParam(required = false) Long equipmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Booking> pending = bookingService.getPendingBookings(userDetails.getUsername());
        if (equipmentId != null) {
            pending = pending.stream().filter(b -> b.getEquipment().getId().equals(equipmentId)).toList();
        }
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Booking>> processApproval(
            @PathVariable Long id,
            @RequestBody BookingApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Booking booking = bookingService.processApproval(id, request, userDetails.getUsername());
        String action = request.isApproved() ? "confirmed" : "rejected";
        return ResponseEntity.ok(ApiResponse.ok("Booking " + action + " successfully", booking));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingCancelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = request != null ? request.getReason() : "Cancelled by user";
        Booking booking = bookingService.cancelBooking(id, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", booking));
    }

    @PostMapping("/{id}/shift-to-now")
    public ResponseEntity<ApiResponse<Booking>> shiftToNow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Booking booking = bookingService.shiftToCurrentTime(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Booking shifted to current window for live Check-In demo.", booking));
    }

    // --- Recurring Bookings Endpoints (Task T4) ---

    @PostMapping("/recurring/preview")
    public ResponseEntity<ApiResponse<RecurringBookingPreviewResponse>> previewRecurring(
            @Valid @RequestBody RecurringBookingRequest request,
            @AuthenticationPrincipal UserPrincipal caller) {
        RecurringBookingPreviewResponse preview = recurringBookingService.previewRecurring(request, caller);
        return ResponseEntity.ok(ApiResponse.ok(preview));
    }

    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN', 'ROLE_RESEARCHER_STUDENT', 'ROLE_DEPARTMENT_HEAD', 'ROLE_SYSTEM_ADMIN', 'RESEARCHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<Booking>>> createRecurring(
            @Valid @RequestBody RecurringBookingRequest request,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<Booking> created = recurringBookingService.createRecurring(request, caller);
        return ResponseEntity.ok(ApiResponse.ok("Created recurring booking series with " + created.size() + " occurrences.", created));
    }

    @GetMapping("/series/{recurrenceId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getSeriesDetails(@PathVariable String recurrenceId) {
        List<Booking> series = recurringBookingService.getSeriesBookings(recurrenceId);
        return ResponseEntity.ok(ApiResponse.ok(series));
    }

    @PostMapping("/series/{recurrenceId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelSeries(
            @PathVariable String recurrenceId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String reason = (body != null) ? body.get("reason") : null;
        int count = recurringBookingService.cancelSeries(recurrenceId, reason, caller);
        return ResponseEntity.ok(ApiResponse.ok("Series cancelled successfully", Map.of("cancelledCount", count)));
    }

    @PostMapping("/series/{recurrenceId}/approve")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveSeries(
            @PathVariable String recurrenceId,
            @AuthenticationPrincipal UserPrincipal caller) {
        int count = recurringBookingService.approveSeries(recurrenceId, caller);
        return ResponseEntity.ok(ApiResponse.ok("Recurring series approved successfully", Map.of("approvedCount", count)));
    }

    @PostMapping("/series/{recurrenceId}/reject")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectSeries(
            @PathVariable String recurrenceId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String reason = (body != null) ? body.get("reason") : null;
        int count = recurringBookingService.rejectSeries(recurrenceId, reason, caller);
        return ResponseEntity.ok(ApiResponse.ok("Recurring series rejected successfully", Map.of("rejectedCount", count)));
    }

    // --- Booking Suggestions Endpoint (Task T4) ---

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<BookingSuggestionResponse>> getSuggestions(
            @RequestParam Long equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @AuthenticationPrincipal UserPrincipal caller) {
        BookingSuggestionResponse suggestions = bookingSuggestionService.getSuggestions(equipmentId, startTime, endTime, caller);
        return ResponseEntity.ok(ApiResponse.ok(suggestions));
    }
}
