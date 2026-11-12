package com.lab.platform.sharing;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sharing")
public class SharingController {

    private final SharingService sharingService;

    public SharingController(SharingService sharingService) {
        this.sharingService = sharingService;
    }

    // =========================================================================
    // 1. Listings Management & Discovery
    // =========================================================================

    @PostMapping("/listings")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SharingListing>> createOrUpdateListing(
            @Valid @RequestBody SharingListingDto dto,
            @AuthenticationPrincipal UserPrincipal caller) {
        SharingListing listing = sharingService.createOrUpdateListing(dto, caller);
        return ResponseEntity.ok(ApiResponse.ok("Equipment sharing listing saved successfully", listing));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<SharingListing>>> searchListings(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<SharingListing> listings = sharingService.searchListings(institutionId, category, keyword);
        return ResponseEntity.ok(ApiResponse.ok(listings));
    }

    @GetMapping("/listings/equipment/{equipmentId}")
    public ResponseEntity<ApiResponse<SharingListing>> getListingByEquipment(@PathVariable Long equipmentId) {
        return sharingService.getListingByEquipmentId(equipmentId)
                .map(l -> ResponseEntity.ok(ApiResponse.ok(l)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PutMapping("/listings/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SharingListing>> toggleListingActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserPrincipal caller) {
        boolean active = body.getOrDefault("active", true);
        SharingListing updated = sharingService.toggleListingActive(id, active, caller);
        return ResponseEntity.ok(ApiResponse.ok("Listing active status updated", updated));
    }

    // =========================================================================
    // 2. Access Requests (2-Step Approval Workflow)
    // =========================================================================

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD')")
    public ResponseEntity<ApiResponse<AccessRequest>> submitAccessRequest(
            @Valid @RequestBody AccessRequestCreateDto dto,
            @AuthenticationPrincipal UserPrincipal caller) {
        AccessRequest request = sharingService.submitAccessRequest(dto, caller);
        return ResponseEntity.ok(ApiResponse.ok("Access request submitted. Awaiting home institution endorsement.", request));
    }

    @GetMapping("/requests/my")
    public ResponseEntity<ApiResponse<List<AccessRequest>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<AccessRequest> requests = sharingService.getMyRequests(caller);
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @GetMapping("/requests/outgoing")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AccessRequest>>> getOutgoingRequests(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<AccessRequest> requests = sharingService.getOutgoingRequests(caller);
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @GetMapping("/requests/incoming")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AccessRequest>>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<AccessRequest> requests = sharingService.getIncomingRequests(caller);
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @PutMapping("/requests/{id}/forward")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AccessRequest>> forwardRequest(
            @PathVariable Long id,
            @RequestBody(required = false) AccessRequestDecisionDto body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String notes = (body != null) ? body.getNotes() : null;
        AccessRequest request = sharingService.forwardRequestByHome(id, notes, caller);
        return ResponseEntity.ok(ApiResponse.ok("Request endorsed and forwarded to equipment host institution", request));
    }

    @PutMapping("/requests/{id}/reject-home")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AccessRequest>> rejectByHome(
            @PathVariable Long id,
            @RequestBody(required = false) AccessRequestDecisionDto body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String notes = (body != null) ? body.getNotes() : null;
        AccessRequest request = sharingService.rejectRequestByHome(id, notes, caller);
        return ResponseEntity.ok(ApiResponse.ok("Request declined by home institution", request));
    }

    @PutMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AccessRequest>> approveByHost(
            @PathVariable Long id,
            @RequestBody(required = false) AccessRequestDecisionDto body,
            @AuthenticationPrincipal UserPrincipal caller) {
        AccessRequest request = sharingService.approveRequestByHost(
                id,
                body != null ? body.getValidFrom() : null,
                body != null ? body.getValidTo() : null,
                body != null ? body.getNotes() : null,
                caller
        );
        return ResponseEntity.ok(ApiResponse.ok("Access request approved and Access Grant issued", request));
    }

    @PutMapping("/requests/{id}/reject-host")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AccessRequest>> rejectByHost(
            @PathVariable Long id,
            @RequestBody(required = false) AccessRequestDecisionDto body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String notes = (body != null) ? body.getNotes() : null;
        AccessRequest request = sharingService.rejectRequestByHost(id, notes, caller);
        return ResponseEntity.ok(ApiResponse.ok("Request declined by equipment host institution", request));
    }

    @PutMapping("/requests/{id}/withdraw")
    public ResponseEntity<ApiResponse<AccessRequest>> withdrawRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        AccessRequest request = sharingService.withdrawRequest(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("Access request withdrawn", request));
    }

    // =========================================================================
    // 3. Agreements & Grants
    // =========================================================================

    @GetMapping("/agreements")
    public ResponseEntity<ApiResponse<List<SharingAgreement>>> getAgreements(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<SharingAgreement> agreements = sharingService.getAgreementsForInstitution(caller);
        return ResponseEntity.ok(ApiResponse.ok(agreements));
    }

    @GetMapping("/my-grants")
    public ResponseEntity<ApiResponse<List<AccessGrant>>> getMyGrants(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<AccessGrant> grants = sharingService.getMyActiveGrants(caller);
        return ResponseEntity.ok(ApiResponse.ok(grants));
    }

    @PutMapping("/agreements/{id}/revoke")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SharingAgreement>> revokeAgreement(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal caller) {
        String reason = (body != null) ? body.get("reason") : null;
        SharingAgreement revoked = sharingService.revokeAgreement(id, reason, caller);
        return ResponseEntity.ok(ApiResponse.ok("Sharing agreement and associated grants revoked", revoked));
    }

    // =========================================================================
    // 4. Shared Schedule View API with Privacy Masking
    // =========================================================================

    @GetMapping("/equipment/{equipmentId}/schedule")
    public ResponseEntity<ApiResponse<List<SharedScheduleSlotDto>>> getSharedSchedule(
            @PathVariable Long equipmentId,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<SharedScheduleSlotDto> schedule = sharingService.getSharedEquipmentSchedule(equipmentId, caller);
        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }

    // =========================================================================
    // 5. Itemized Usage Fees API
    // =========================================================================

    @GetMapping("/agreements/{id}/fees")
    public ResponseEntity<ApiResponse<List<ItemizedFeeItemDto>>> getItemizedFees(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<ItemizedFeeItemDto> fees = sharingService.getItemizedFeesForAgreement(id, caller);
        return ResponseEntity.ok(ApiResponse.ok(fees));
    }
}
