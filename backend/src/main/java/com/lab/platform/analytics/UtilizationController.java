package com.lab.platform.analytics;

import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UtilizationController {

    private final UtilizationService utilizationService;
    private final OperatingHoursService operatingHoursService;

    public UtilizationController(UtilizationService utilizationService,
                                 OperatingHoursService operatingHoursService) {
        this.utilizationService = utilizationService;
        this.operatingHoursService = operatingHoursService;
    }

    @GetMapping("/analytics/utilization")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UtilizationSummaryResponse>> getUtilizationSummary(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal caller) {
        UtilizationSummaryResponse res = utilizationService.getUtilizationSummary(scopeType, scopeId, startDate, endDate, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/benchmark")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BenchmarkResponse>> getBenchmark(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal caller) {
        BenchmarkResponse res = utilizationService.getBenchmark(scopeType, scopeId, startDate, endDate, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/demand")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<DemandItemDto>>> getDemandAnalysis(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean highDemandOnly,
            @AuthenticationPrincipal UserPrincipal caller) {
        List<DemandItemDto> res = utilizationService.getDemandAnalysis(scopeType, scopeId, startDate, endDate, highDemandOnly, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/idle-time")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<IdleTimeAnalysisResponse>> getIdleTime(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal caller) {
        IdleTimeAnalysisResponse res = utilizationService.getIdleTimeAnalysis(scopeType, scopeId, startDate, endDate, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/idle-alerts")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<IdleAlertItem>>> getIdleAlerts(
            @AuthenticationPrincipal UserPrincipal caller) {
        List<IdleAlertItem> res = utilizationService.getIdleAlerts(caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/peak-usage")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PeakUsageAnalysisResponse>> getPeakUsage(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal caller) {
        PeakUsageAnalysisResponse res = utilizationService.getPeakUsageAnalysis(scopeType, scopeId, startDate, endDate, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/analytics/heatmap")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HeatmapDataResponse>> getHeatmapData(
            @RequestParam String scopeType,
            @RequestParam Long scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal caller) {
        HeatmapDataResponse res = utilizationService.getHeatmapData(scopeType, scopeId, startDate, endDate, caller);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping("/equipment/{id}/operating-hours")
    public ResponseEntity<ApiResponse<List<EquipmentOperatingHours>>> getEquipmentOperatingHours(
            @PathVariable Long id) {
        List<EquipmentOperatingHours> hours = operatingHoursService.getOperatingHours(id);
        return ResponseEntity.ok(ApiResponse.ok(hours));
    }

    @PutMapping("/equipment/{id}/operating-hours")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<EquipmentOperatingHours>>> updateEquipmentOperatingHours(
            @PathVariable Long id,
            @Valid @RequestBody List<OperatingHoursDto> dtos) {
        List<EquipmentOperatingHours> saved = operatingHoursService.setOperatingHours(id, dtos);
        return ResponseEntity.ok(ApiResponse.ok("Operating hours updated successfully.", saved));
    }
}
