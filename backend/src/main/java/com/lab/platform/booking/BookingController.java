package com.lab.platform.booking;

import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
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
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Booking>>> getPendingBookings(@AuthenticationPrincipal UserDetails userDetails) {
        List<Booking> pending = bookingService.getPendingBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('ROLE_LAB_MANAGER', 'ROLE_DEPARTMENT_HEAD', 'ROLE_INSTITUTION_ADMIN', 'ROLE_SYSTEM_ADMIN')")
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
}
