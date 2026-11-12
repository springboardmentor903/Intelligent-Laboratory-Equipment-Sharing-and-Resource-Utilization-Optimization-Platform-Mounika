package com.lab.platform.waitlist;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.booking.Booking;
import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN', 'ROLE_RESEARCHER_STUDENT', 'ROLE_DEPARTMENT_HEAD', 'ROLE_SYSTEM_ADMIN', 'RESEARCHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<WaitlistResponse>> joinWaitlist(
            @Valid @RequestBody WaitlistJoinRequest request,
            @AuthenticationPrincipal UserPrincipal caller) {
        WaitlistResponse response = waitlistService.joinWaitlist(request, caller);
        return ResponseEntity.ok(ApiResponse.ok("Successfully joined the waitlist at queue position " + response.getQueuePosition(), response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'SYSTEM_ADMIN', 'ROLE_RESEARCHER_STUDENT', 'ROLE_DEPARTMENT_HEAD', 'ROLE_SYSTEM_ADMIN', 'RESEARCHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Page<WaitlistResponse>>> getMyWaitlist(
            @RequestParam(required = false) WaitlistStatus status,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal caller) {
        Page<WaitlistResponse> response = waitlistService.getMyWaitlist(status, pageable, caller);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Booking>> acceptOffer(
            @PathVariable Long id,
            @Valid @RequestBody WaitlistAcceptRequest request,
            @AuthenticationPrincipal UserPrincipal caller) {
        Booking booking = waitlistService.acceptOffer(id, request, caller);
        return ResponseEntity.ok(ApiResponse.ok("Waitlist offer accepted! Your reservation is created with status " + booking.getStatus(), booking));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<String>> declineOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        waitlistService.declineOffer(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("Waitlist offer declined. The slot has been released for the next researcher in queue."));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelWaitlist(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        waitlistService.cancelWaitlist(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("You have left the waitlist for this equipment."));
    }
}
