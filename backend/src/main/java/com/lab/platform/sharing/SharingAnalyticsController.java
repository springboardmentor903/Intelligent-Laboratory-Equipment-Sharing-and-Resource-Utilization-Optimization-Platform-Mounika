package com.lab.platform.sharing;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sharing/analytics")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'DEPARTMENT_HEAD', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
public class SharingAnalyticsController {

    private final SharingAnalyticsService analyticsService;

    public SharingAnalyticsController(SharingAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SharingAnalyticsSummaryDto>> getSummary(
            @AuthenticationPrincipal UserPrincipal caller) {
        SharingAnalyticsSummaryDto summary = analyticsService.getSummary(caller);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/partners")
    public ResponseEntity<ApiResponse<List<PartnerSummaryDto>>> getPartners(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<PartnerSummaryDto> partners = analyticsService.getPartners(caller);
        return ResponseEntity.ok(ApiResponse.ok(partners));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<SharingTrendPointDto>>> getTrends(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<SharingTrendPointDto> trends = analyticsService.getTrends(caller, months);
        return ResponseEntity.ok(ApiResponse.ok(trends));
    }

    @GetMapping("/most-shared")
    public ResponseEntity<ApiResponse<List<MostSharedEquipmentDto>>> getMostSharedEquipment(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<MostSharedEquipmentDto> list = analyticsService.getMostSharedEquipment(caller, limit);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/partnerships/{partnerInstitutionId}")
    public ResponseEntity<ApiResponse<PartnershipReportDto>> getPartnershipReport(
            @PathVariable Long partnerInstitutionId,
            @AuthenticationPrincipal UserPrincipal caller) {
        PartnershipReportDto report = analyticsService.getPartnershipReport(partnerInstitutionId, caller);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
