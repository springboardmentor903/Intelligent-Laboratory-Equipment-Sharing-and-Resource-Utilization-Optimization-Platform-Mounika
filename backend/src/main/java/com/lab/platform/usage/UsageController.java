package com.lab.platform.usage;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.booking.Booking;
import com.lab.platform.common.ApiResponse;
import com.lab.platform.equipment.EquipmentStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/bookings/{id}/check-in")
    public ResponseEntity<ApiResponse<Booking>> checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        Booking booking = usageService.checkIn(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("Session started successfully. Equipment is now In Use.", booking));
    }

    @PostMapping("/bookings/{id}/check-out")
    public ResponseEntity<ApiResponse<Booking>> checkOut(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        Booking booking = usageService.checkOut(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("Session completed successfully. Equipment is now available.", booking));
    }

    @GetMapping("/equipment/live-status")
    public ResponseEntity<ApiResponse<List<LiveEquipmentStatusResponse>>> getLiveEquipmentStatus(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<LiveEquipmentStatusResponse> liveStatus = usageService.getLiveEquipmentStatus(
                institutionId, departmentId, status, category, caller
        );
        return ResponseEntity.ok(ApiResponse.ok(liveStatus));
    }
}
