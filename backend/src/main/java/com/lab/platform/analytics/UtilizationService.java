package com.lab.platform.analytics;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.booking.BookingStatus;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentStatus;
import com.lab.platform.institution.Department;
import com.lab.platform.institution.DepartmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.institution.InstitutionRepository;
import com.lab.platform.usage.UsageLog;
import com.lab.platform.usage.UsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UtilizationService {

    private static final Logger logger = LoggerFactory.getLogger(UtilizationService.class);

    private final EquipmentRepository equipmentRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UsageLogRepository usageLogRepository;
    private final BookingRepository bookingRepository;
    private final OperatingHoursService operatingHoursService;
    private final EquipmentOperatingHoursRepository operatingHoursRepository;
    private final DailyUtilizationSnapshotRepository snapshotRepository;

    @Value("${app.timezone:UTC}")
    private String platformTimezone;

    @Value("${app.demand.high-demand-threshold:80}")
    private double highDemandThreshold;

    public UtilizationService(EquipmentRepository equipmentRepository,
                              InstitutionRepository institutionRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              UsageLogRepository usageLogRepository,
                              BookingRepository bookingRepository,
                              OperatingHoursService operatingHoursService,
                              EquipmentOperatingHoursRepository operatingHoursRepository,
                              DailyUtilizationSnapshotRepository snapshotRepository) {
        this.equipmentRepository = equipmentRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.usageLogRepository = usageLogRepository;
        this.bookingRepository = bookingRepository;
        this.operatingHoursService = operatingHoursService;
        this.operatingHoursRepository = operatingHoursRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Utilization Summary (Task T2):
     * Formula: (Total Used Minutes / Total Available Minutes) * 100
     * Based on actual UsageLogs within operating hours.
     */
    @Transactional(readOnly = true)
    public UtilizationSummaryResponse getUtilizationSummary(String scopeType, Long scopeId,
                                                            LocalDate startDate, LocalDate endDate,
                                                            UserPrincipal caller) {
        ScopeResolution scope = resolveAndVerifyScope(scopeType, scopeId, caller);
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();

        long totalAvailableMinutes = 0;
        long totalUsedMinutes = 0;
        long totalSharedUsedMinutes = 0;

        List<Long> equipmentIds = scope.equipments.stream().map(Equipment::getId).toList();
        if (equipmentIds.isEmpty()) {
            UtilizationSummaryResponse empty = new UtilizationSummaryResponse();
            empty.setScopeType(scopeType);
            empty.setScopeId(scopeId);
            empty.setScopeName(scope.name);
            empty.setStartDate(startDate);
            empty.setEndDate(endDate);
            empty.setPlatformTimezone(platformTimezone);
            return empty;
        }

        // Fetch operating hours and usage logs in bulk to prevent N+1 query overhead
        List<EquipmentOperatingHours> allHours = operatingHoursRepository.findByEquipmentIdInAndActiveTrue(equipmentIds);
        Map<Long, Map<Integer, EquipmentOperatingHours>> hoursByEq = new HashMap<>();
        for (EquipmentOperatingHours h : allHours) {
            hoursByEq.computeIfAbsent(h.getEquipment().getId(), k -> new HashMap<>())
                    .put(h.getDayOfWeek(), h);
        }

        List<UsageLog> allUsageLogs = usageLogRepository.findOverlappingUsageForEquipmentIds(equipmentIds, startDateTime, endDateTime);
        Map<Long, List<UsageLog>> logsByEq = allUsageLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getEquipment().getId()));

        for (Equipment eq : scope.equipments) {
            Map<Integer, EquipmentOperatingHours> opHours = hoursByEq.computeIfAbsent(eq.getId(), k -> getDefaultOperatingHoursMap(eq));

            long eqAvailableMins = operatingHoursService.calculateAvailableMinutes(eq.getId(), startDateTime, endDateTime);
            totalAvailableMinutes += eqAvailableMins;

            List<UsageLog> logs = logsByEq.getOrDefault(eq.getId(), Collections.emptyList());
            for (UsageLog log : logs) {
                LocalDateTime sessionStart = log.getActualStartTime().isBefore(startDateTime) ? startDateTime : log.getActualStartTime();
                LocalDateTime rawEnd = (log.getActualEndTime() != null) ? log.getActualEndTime() : now;
                LocalDateTime sessionEnd = rawEnd.isAfter(endDateTime) ? endDateTime : rawEnd;

                if (sessionEnd.isAfter(sessionStart)) {
                    long usedInHours = operatingHoursService.calculateMinutesWithinOperatingHours(eq.getId(), sessionStart, sessionEnd, opHours);
                    totalUsedMinutes += usedInHours;
                    if (log.getUsageType() == UsageLog.UsageType.SHARED) {
                        totalSharedUsedMinutes += usedInHours;
                    }
                }
            }
        }

        UtilizationSummaryResponse response = new UtilizationSummaryResponse();
        response.setScopeType(scopeType);
        response.setScopeId(scopeId);
        response.setScopeName(scope.name);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setPlatformTimezone(platformTimezone);

        response.setTotalAvailableMinutes(totalAvailableMinutes);
        response.setTotalAvailableHours(roundHours(totalAvailableMinutes));
        response.setTotalUsedMinutes(totalUsedMinutes);
        response.setTotalUsedHours(roundHours(totalUsedMinutes));

        if (totalAvailableMinutes > 0) {
            BigDecimal rate = BigDecimal.valueOf(totalUsedMinutes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalAvailableMinutes), 2, RoundingMode.HALF_UP);
            response.setUtilizationRate(rate);

            BigDecimal target = response.getTargetPercentage();
            BigDecimal gap = rate.subtract(target);
            response.setTargetGap(gap);
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                response.setTargetStatus("EXCEEDED");
            } else if (gap.compareTo(BigDecimal.valueOf(-5.0)) >= 0) {
                response.setTargetStatus("ON_TRACK");
            } else {
                response.setTargetStatus("BELOW_TARGET");
            }
        } else {
            response.setUtilizationRate(null);
            response.setTargetStatus("N/A");
        }

        response.setSharedUsedMinutes(totalSharedUsedMinutes);
        if (totalUsedMinutes > 0) {
            BigDecimal sharedPct = BigDecimal.valueOf(totalSharedUsedMinutes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalUsedMinutes), 2, RoundingMode.HALF_UP);
            response.setSharedUsagePercentage(sharedPct);
        } else {
            response.setSharedUsagePercentage(BigDecimal.ZERO);
        }

        return response;
    }

    /**
     * Benchmark API (Task T2):
     * Compares selected period's rate with the preceding 28 days.
     */
    @Transactional(readOnly = true)
    public BenchmarkResponse getBenchmark(String scopeType, Long scopeId,
                                          LocalDate startDate, LocalDate endDate,
                                          UserPrincipal caller) {
        UtilizationSummaryResponse current = getUtilizationSummary(scopeType, scopeId, startDate, endDate, caller);

        // Preceding 28 days benchmark
        LocalDate benchmarkEnd = startDate.minusDays(1);
        LocalDate benchmarkStart = benchmarkEnd.minusDays(27);
        UtilizationSummaryResponse benchmark = getUtilizationSummary(scopeType, scopeId, benchmarkStart, benchmarkEnd, caller);

        BigDecimal currentRate = current.getUtilizationRate() != null ? current.getUtilizationRate() : BigDecimal.ZERO;
        BigDecimal benchmarkRate = benchmark.getUtilizationRate() != null ? benchmark.getUtilizationRate() : BigDecimal.ZERO;
        BigDecimal diff = currentRate.subtract(benchmarkRate);

        String status = "STABLE";
        if (diff.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            status = "INCREASED";
        } else if (diff.compareTo(BigDecimal.valueOf(-0.5)) < 0) {
            status = "DECREASED";
        }

        return new BenchmarkResponse(currentRate, benchmarkRate, diff, status);
    }

    /**
     * Demand Analysis API (Task T2):
     * Computes booked minutes, booking rate, no-show rate, cancellation rate, and high demand status.
     */
    @Transactional(readOnly = true)
    public List<DemandItemDto> getDemandAnalysis(String scopeType, Long scopeId,
                                                LocalDate startDate, LocalDate endDate,
                                                boolean highDemandOnly,
                                                UserPrincipal caller) {
        ScopeResolution scope = resolveAndVerifyScope(scopeType, scopeId, caller);
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);

        List<DemandItemDto> results = new ArrayList<>();
        List<Long> equipmentIds = scope.equipments.stream().map(Equipment::getId).toList();
        if (equipmentIds.isEmpty()) {
            return results;
        }

        List<BookingStatus> allStatuses = Arrays.asList(BookingStatus.values());
        List<Booking> bookings = bookingRepository.findOverlappingBookingsForEquipmentIds(equipmentIds, allStatuses, startDateTime, endDateTime);
        Map<Long, List<Booking>> bookingsByEq = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getEquipment().getId()));

        for (Equipment eq : scope.equipments) {
            long availableMinutes = operatingHoursService.calculateAvailableMinutes(eq.getId(), startDateTime, endDateTime);
            List<Booking> eqBookings = bookingsByEq.getOrDefault(eq.getId(), Collections.emptyList());

            long bookedMinutes = 0;
            long completedCount = 0;
            long inUseCount = 0;
            long noShowCount = 0;
            long cancelledCount = 0;

            Map<Integer, EquipmentOperatingHours> opHours = getDefaultOperatingHoursMap(eq);

            for (Booking b : eqBookings) {
                if (b.getStatus() == BookingStatus.COMPLETED) completedCount++;
                else if (b.getStatus() == BookingStatus.IN_USE) inUseCount++;
                else if (b.getStatus() == BookingStatus.NO_SHOW) noShowCount++;
                else if (b.getStatus() == BookingStatus.CANCELLED) cancelledCount++;

                // Booked minutes include PENDING_APPROVAL, CONFIRMED, IN_USE, COMPLETED
                if (b.getStatus() == BookingStatus.PENDING_APPROVAL ||
                    b.getStatus() == BookingStatus.CONFIRMED ||
                    b.getStatus() == BookingStatus.IN_USE ||
                    b.getStatus() == BookingStatus.COMPLETED) {

                    LocalDateTime bStart = b.getStartTime().isBefore(startDateTime) ? startDateTime : b.getStartTime();
                    LocalDateTime bEnd = b.getEndTime().isAfter(endDateTime) ? endDateTime : b.getEndTime();
                    if (bEnd.isAfter(bStart)) {
                        bookedMinutes += operatingHoursService.calculateMinutesWithinOperatingHours(eq.getId(), bStart, bEnd, opHours);
                    }
                }
            }

            DemandItemDto dto = new DemandItemDto();
            dto.setEquipmentId(eq.getId());
            dto.setEquipmentName(eq.getName());
            dto.setAssetTag(eq.getAssetTag());
            dto.setDepartmentName(eq.getDepartment().getName());
            dto.setTotalBookings(eqBookings.size());
            dto.setBookedMinutes(bookedMinutes);
            dto.setAvailableMinutes(availableMinutes);

            BigDecimal bookingRate = BigDecimal.ZERO;
            if (availableMinutes > 0) {
                bookingRate = BigDecimal.valueOf(bookedMinutes)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(availableMinutes), 2, RoundingMode.HALF_UP);
            }
            dto.setBookingRate(bookingRate);

            long finishedSessions = completedCount + inUseCount + noShowCount;
            BigDecimal noShowRate = (finishedSessions > 0)
                    ? BigDecimal.valueOf(noShowCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(finishedSessions), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            dto.setNoShowRate(noShowRate);

            BigDecimal cancellationRate = (!eqBookings.isEmpty())
                    ? BigDecimal.valueOf(cancelledCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(eqBookings.size()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            dto.setCancellationRate(cancellationRate);

            boolean isHighDemand = bookingRate.doubleValue() >= highDemandThreshold;
            dto.setHighDemand(isHighDemand);
            dto.setWaitlistCount(0); // Populated if waitlists active

            if (!highDemandOnly || isHighDemand) {
                results.add(dto);
            }
        }

        results.sort(Comparator.comparing(DemandItemDto::getBookingRate).reversed());
        return results;
    }

    /**
     * Idle Time Analysis API (Task T3):
     * Idle minutes = Available minutes - Booked minutes during operating hours.
     */
    @Transactional(readOnly = true)
    public IdleTimeAnalysisResponse getIdleTimeAnalysis(String scopeType, Long scopeId,
                                                        LocalDate startDate, LocalDate endDate,
                                                        UserPrincipal caller) {
        ScopeResolution scope = resolveAndVerifyScope(scopeType, scopeId, caller);
        validateDateRange(startDate, endDate);

        long totalAvailableMinutes = 0;
        long totalBookedMinutes = 0;
        List<IdleTimeAnalysisResponse.DailyIdleRecord> dailyRecords = new ArrayList<>();

        LocalDate curDate = startDate;
        while (!curDate.isAfter(endDate)) {
            LocalDateTime dayStart = LocalDateTime.of(curDate, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(curDate, LocalTime.MAX);

            long dayAvail = 0;
            long dayBooked = 0;

            for (Equipment eq : scope.equipments) {
                dayAvail += operatingHoursService.calculateAvailableMinutes(eq.getId(), dayStart, dayEnd);
                Map<Integer, EquipmentOperatingHours> opHours = getDefaultOperatingHoursMap(eq);

                List<Booking> dayBookings = bookingRepository.findOverlappingBookingsForEquipment(
                        eq.getId(),
                        List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED, BookingStatus.IN_USE, BookingStatus.COMPLETED),
                        dayStart, dayEnd
                );

                for (Booking b : dayBookings) {
                    LocalDateTime bStart = b.getStartTime().isBefore(dayStart) ? dayStart : b.getStartTime();
                    LocalDateTime bEnd = b.getEndTime().isAfter(dayEnd) ? dayEnd : b.getEndTime();
                    if (bEnd.isAfter(bStart)) {
                        dayBooked += operatingHoursService.calculateMinutesWithinOperatingHours(eq.getId(), bStart, bEnd, opHours);
                    }
                }
            }

            long dayIdle = Math.max(0, dayAvail - dayBooked);
            BigDecimal dayIdlePct = (dayAvail > 0)
                    ? BigDecimal.valueOf(dayIdle).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(dayAvail), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            dailyRecords.add(new IdleTimeAnalysisResponse.DailyIdleRecord(curDate, dayAvail, dayBooked, dayIdle, dayIdlePct));
            totalAvailableMinutes += dayAvail;
            totalBookedMinutes += dayBooked;

            curDate = curDate.plusDays(1);
        }

        long totalIdleMinutes = Math.max(0, totalAvailableMinutes - totalBookedMinutes);
        BigDecimal idlePercentage = (totalAvailableMinutes > 0)
                ? BigDecimal.valueOf(totalIdleMinutes).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalAvailableMinutes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        IdleTimeAnalysisResponse response = new IdleTimeAnalysisResponse();
        response.setTotalAvailableMinutes(totalAvailableMinutes);
        response.setTotalAvailableHours(roundHours(totalAvailableMinutes));
        response.setTotalIdleMinutes(totalIdleMinutes);
        response.setTotalIdleHours(roundHours(totalIdleMinutes));
        response.setIdlePercentage(idlePercentage);
        response.setDailyBreakdown(dailyRecords);

        return response;
    }

    /**
     * Idle Alerts API (Task T3):
     * Equipment with idle percentage >= 90% over last 7 completed days.
     * Only considers AVAILABLE or BOOKED status; excludes UNDER_MAINTENANCE, OUT_OF_SERVICE, RETIRED.
     */
    @Transactional(readOnly = true)
    public List<IdleAlertItem> getIdleAlerts(UserPrincipal caller) {
        verifyAnalyticsAccess(caller);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate sevenDaysAgo = yesterday.minusDays(6);

        LocalDateTime windowStart = LocalDateTime.of(sevenDaysAgo, LocalTime.MIN);
        LocalDateTime windowEnd = LocalDateTime.of(yesterday, LocalTime.MAX);

        List<Equipment> candidates = equipmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == EquipmentStatus.AVAILABLE || e.getStatus() == EquipmentStatus.BOOKED)
                .filter(e -> hasAccessToEquipment(caller, e))
                .toList();

        List<IdleAlertItem> alerts = new ArrayList<>();

        for (Equipment eq : candidates) {
            long availMins = operatingHoursService.calculateAvailableMinutes(eq.getId(), windowStart, windowEnd);
            if (availMins == 0) continue;

            List<Booking> bookings = bookingRepository.findOverlappingBookingsForEquipment(
                    eq.getId(),
                    List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED, BookingStatus.IN_USE, BookingStatus.COMPLETED),
                    windowStart, windowEnd
            );

            Map<Integer, EquipmentOperatingHours> opHours = getDefaultOperatingHoursMap(eq);
            long bookedMins = 0;
            LocalDateTime lastBookingEnd = null;

            for (Booking b : bookings) {
                LocalDateTime bStart = b.getStartTime().isBefore(windowStart) ? windowStart : b.getStartTime();
                LocalDateTime bEnd = b.getEndTime().isAfter(windowEnd) ? windowEnd : b.getEndTime();
                if (bEnd.isAfter(bStart)) {
                    bookedMins += operatingHoursService.calculateMinutesWithinOperatingHours(eq.getId(), bStart, bEnd, opHours);
                }
                if (lastBookingEnd == null || b.getEndTime().isAfter(lastBookingEnd)) {
                    lastBookingEnd = b.getEndTime();
                }
            }

            long idleMins = Math.max(0, availMins - bookedMins);
            BigDecimal idleRate = BigDecimal.valueOf(idleMins)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(availMins), 2, RoundingMode.HALF_UP);

            if (idleRate.doubleValue() >= 90.0) {
                alerts.add(new IdleAlertItem(
                        eq.getId(),
                        eq.getName(),
                        eq.getAssetTag(),
                        eq.getDepartment().getName(),
                        idleRate,
                        roundHours(idleMins),
                        lastBookingEnd
                ));
            }
        }

        alerts.sort(Comparator.comparing(IdleAlertItem::getIdlePercentage).reversed());
        return alerts;
    }

    /**
     * Peak Usage Analysis (Task T3):
     * Day of week patterns, hour of day patterns, and top 5 busiest slots.
     */
    @Transactional(readOnly = true)
    public PeakUsageAnalysisResponse getPeakUsageAnalysis(String scopeType, Long scopeId,
                                                          LocalDate startDate, LocalDate endDate,
                                                          UserPrincipal caller) {
        ScopeResolution scope = resolveAndVerifyScope(scopeType, scopeId, caller);
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);

        Map<Integer, Long> dayOfWeekUsage = new HashMap<>();
        for (int i = 1; i <= 7; i++) dayOfWeekUsage.put(i, 0L);

        Map<Integer, Long> hourOfDayUsage = new HashMap<>();
        for (int i = 0; i <= 23; i++) hourOfDayUsage.put(i, 0L);

        // Day and hour slots matrix [day 1..7][hour 0..23]
        long[][] slotMatrix = new long[8][24];

        List<Long> equipmentIds = scope.equipments.stream().map(Equipment::getId).toList();
        if (!equipmentIds.isEmpty()) {
            List<UsageLog> logs = usageLogRepository.findOverlappingUsageForEquipmentIds(equipmentIds, startDateTime, endDateTime);
            LocalDateTime now = LocalDateTime.now();

            for (UsageLog log : logs) {
                LocalDateTime start = log.getActualStartTime().isBefore(startDateTime) ? startDateTime : log.getActualStartTime();
                LocalDateTime rawEnd = (log.getActualEndTime() != null) ? log.getActualEndTime() : now;
                LocalDateTime end = rawEnd.isAfter(endDateTime) ? endDateTime : rawEnd;

                LocalDateTime cursor = start;
                while (cursor.isBefore(end)) {
                    LocalDateTime nextHour = cursor.plusHours(1).withMinute(0).withSecond(0).withNano(0);
                    LocalDateTime chunkEnd = end.isBefore(nextHour) ? end : nextHour;

                    long chunkMins = Duration.between(cursor, chunkEnd).toMinutes();
                    int dow = cursor.getDayOfWeek().getValue();
                    int hour = cursor.getHour();

                    dayOfWeekUsage.put(dow, dayOfWeekUsage.get(dow) + chunkMins);
                    hourOfDayUsage.put(hour, hourOfDayUsage.get(hour) + chunkMins);
                    slotMatrix[dow][hour] += chunkMins;

                    cursor = chunkEnd;
                }
            }
        }

        int busiestDay = 1;
        long maxDayMins = -1;
        for (int d = 1; d <= 7; d++) {
            if (dayOfWeekUsage.get(d) > maxDayMins) {
                maxDayMins = dayOfWeekUsage.get(d);
                busiestDay = d;
            }
        }

        int peakHour = 0;
        long maxHourMins = -1;
        for (int h = 0; h <= 23; h++) {
            if (hourOfDayUsage.get(h) > maxHourMins) {
                maxHourMins = hourOfDayUsage.get(h);
                peakHour = h;
            }
        }

        // Top 5 busiest day-and-hour slots
        List<PeakUsageAnalysisResponse.PeakSlotDto> allSlots = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            for (int h = 0; h <= 23; h++) {
                long used = slotMatrix[d][h];
                if (used > 0) {
                    allSlots.add(new PeakUsageAnalysisResponse.PeakSlotDto(
                            d,
                            DayOfWeek.of(d).name(),
                            h,
                            used,
                            BigDecimal.valueOf(used).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(Math.max(1, scope.equipments.size() * 60L)), 2, RoundingMode.HALF_UP)
                    ));
                }
            }
        }

        allSlots.sort((a, b) -> {
            int cmp = Long.compare(b.getUsedMinutes(), a.getUsedMinutes());
            if (cmp != 0) return cmp;
            int dayCmp = Integer.compare(a.getDayOfWeek(), b.getDayOfWeek());
            if (dayCmp != 0) return dayCmp;
            return Integer.compare(a.getHourOfDay(), b.getHourOfDay());
        });

        PeakUsageAnalysisResponse resp = new PeakUsageAnalysisResponse();
        resp.setBusiestDayOfWeek(busiestDay);
        resp.setBusiestDayName(DayOfWeek.of(busiestDay).name());
        resp.setDayOfWeekUsageMinutes(dayOfWeekUsage);
        resp.setPeakHourOfDay(peakHour);
        resp.setHourOfDayUsageMinutes(hourOfDayUsage);
        resp.setTop5BusiestSlots(allSlots.stream().limit(5).collect(Collectors.toList()));

        return resp;
    }

    /**
     * Heatmap Data API (Task T3):
     * Exactly 168 slots (7 days x 24 hours).
     * Closed = null, Open unused = 0%, Open used = percentage.
     */
    @Transactional(readOnly = true)
    public HeatmapDataResponse getHeatmapData(String scopeType, Long scopeId,
                                              LocalDate startDate, LocalDate endDate,
                                              UserPrincipal caller) {
        ScopeResolution scope = resolveAndVerifyScope(scopeType, scopeId, caller);
        validateDateRange(startDate, endDate);

        HeatmapDataResponse response = new HeatmapDataResponse(platformTimezone);
        List<HeatmapDataResponse.HeatmapSlot> slots = new ArrayList<>();

        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();

        List<Long> equipmentIds = scope.equipments.stream().map(Equipment::getId).toList();
        List<UsageLog> usageLogs = usageLogRepository.findOverlappingUsageForEquipmentIds(equipmentIds, startDateTime, endDateTime);

        // Precompute total usage minutes per [dayOfWeek 1..7][hour 0..23]
        long[][] usedMatrix = new long[8][24];
        for (UsageLog log : usageLogs) {
            LocalDateTime start = log.getActualStartTime().isBefore(startDateTime) ? startDateTime : log.getActualStartTime();
            LocalDateTime rawEnd = (log.getActualEndTime() != null) ? log.getActualEndTime() : now;
            LocalDateTime end = rawEnd.isAfter(endDateTime) ? endDateTime : rawEnd;

            LocalDateTime cursor = start;
            while (cursor.isBefore(end)) {
                LocalDateTime nextHour = cursor.plusHours(1).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime chunkEnd = end.isBefore(nextHour) ? end : nextHour;

                long mins = Duration.between(cursor, chunkEnd).toMinutes();
                usedMatrix[cursor.getDayOfWeek().getValue()][cursor.getHour()] += mins;
                cursor = chunkEnd;
            }
        }

        // Count how many occurrences of each day-of-week exist between startDate and endDate
        int[] dowOccurrences = new int[8];
        LocalDate dCursor = startDate;
        while (!dCursor.isAfter(endDate)) {
            dowOccurrences[dCursor.getDayOfWeek().getValue()]++;
            dCursor = dCursor.plusDays(1);
        }

        for (int dow = 1; dow <= 7; dow++) {
            String dayName = DayOfWeek.of(dow).name();
            int occurrences = dowOccurrences[dow];

            for (int hour = 0; hour <= 23; hour++) {
                // Check operating hours across equipment
                long slotAvailableMins = 0;
                for (Equipment eq : scope.equipments) {
                    Map<Integer, EquipmentOperatingHours> opHours = getDefaultOperatingHoursMap(eq);
                    EquipmentOperatingHours dayHours = opHours.get(dow);
                    if (dayHours != null) {
                        LocalTime slotOpen = LocalTime.of(hour, 0);
                        LocalTime slotClose = (hour == 23) ? LocalTime.of(23, 59, 59) : LocalTime.of(hour + 1, 0);

                        LocalTime effOpen = slotOpen.isAfter(dayHours.getOpenTime()) ? slotOpen : dayHours.getOpenTime();
                        LocalTime effClose = slotClose.isBefore(dayHours.getCloseTime()) ? slotClose : dayHours.getCloseTime();

                        if (effClose.isAfter(effOpen)) {
                            slotAvailableMins += Duration.between(effOpen, effClose).toMinutes() * occurrences;
                        }
                    }
                }

                boolean isOpen = slotAvailableMins > 0;
                long usedMins = usedMatrix[dow][hour];

                BigDecimal utilizationPct = null;
                if (isOpen) {
                    if (usedMins == 0) {
                        utilizationPct = BigDecimal.ZERO;
                    } else {
                        utilizationPct = BigDecimal.valueOf(usedMins)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(slotAvailableMins), 2, RoundingMode.HALF_UP);
                    }
                }

                slots.add(new HeatmapDataResponse.HeatmapSlot(
                        dow, dayName, hour, isOpen, slotAvailableMins, usedMins, utilizationPct
                ));
            }
        }

        response.setSlots(slots);
        return response;
    }

    /**
     * Daily Snapshot Scheduled Job:
     * Runs at 00:30 every day to record utilization snapshots.
     */
    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void runDailySnapshotJob() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.info("Starting Daily Utilization Snapshot Job for date: {}", yesterday);

        List<Equipment> allEquipment = equipmentRepository.findAll();
        for (Equipment eq : allEquipment) {
            recordSnapshotForEquipmentAndDate(eq, yesterday);
        }
        logger.info("Daily Utilization Snapshot Job completed successfully.");
    }

    private void recordSnapshotForEquipmentAndDate(Equipment eq, LocalDate date) {
        LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

        long availMins = operatingHoursService.calculateAvailableMinutes(eq.getId(), start, end);
        Map<Integer, EquipmentOperatingHours> opHours = getDefaultOperatingHoursMap(eq);

        List<UsageLog> logs = usageLogRepository.findOverlappingUsage(eq.getId(), start, end);
        long usedMins = 0;
        for (UsageLog log : logs) {
            LocalDateTime s = log.getActualStartTime().isBefore(start) ? start : log.getActualStartTime();
            LocalDateTime e = (log.getActualEndTime() != null && log.getActualEndTime().isBefore(end)) ? log.getActualEndTime() : end;
            if (e.isAfter(s)) {
                usedMins += operatingHoursService.calculateMinutesWithinOperatingHours(eq.getId(), s, e, opHours);
            }
        }

        BigDecimal rate = (availMins > 0)
                ? BigDecimal.valueOf(usedMins).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(availMins), 2, RoundingMode.HALF_UP)
                : null;

        DailyUtilizationSnapshot snapshot = snapshotRepository.findByEquipmentIdAndSnapshotDate(eq.getId(), date)
                .orElse(new DailyUtilizationSnapshot());

        snapshot.setEquipmentId(eq.getId());
        snapshot.setDepartmentId(eq.getDepartment().getId());
        snapshot.setInstitutionId(eq.getInstitution().getId());
        snapshot.setSnapshotDate(date);
        snapshot.setAvailableMinutes(availMins);
        snapshot.setUsedMinutes(usedMins);
        snapshot.setUtilizationRate(rate);

        snapshotRepository.save(snapshot);
    }

    // --- Private Helper Methods ---

    private static class ScopeResolution {
        String name;
        List<Equipment> equipments;
    }

    private ScopeResolution resolveAndVerifyScope(String scopeType, Long scopeId, UserPrincipal caller) {
        verifyAnalyticsAccess(caller);
        ScopeResolution res = new ScopeResolution();

        if ("EQUIPMENT".equalsIgnoreCase(scopeType)) {
            Equipment eq = equipmentRepository.findById(scopeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + scopeId));
            verifyEquipmentScope(caller, eq);
            res.name = eq.getName();
            res.equipments = List.of(eq);
        } else if ("DEPARTMENT".equalsIgnoreCase(scopeType)) {
            Department dept = departmentRepository.findById(scopeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + scopeId));
            verifyDepartmentScope(caller, dept);
            res.name = dept.getName();
            res.equipments = equipmentRepository.findByDepartmentId(scopeId);
        } else if ("INSTITUTION".equalsIgnoreCase(scopeType)) {
            Institution inst = institutionRepository.findById(scopeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + scopeId));
            verifyInstitutionScope(caller, inst);
            res.name = inst.getName();
            res.equipments = equipmentRepository.findByInstitutionId(scopeId);
        } else {
            throw new BadRequestException("Invalid scopeType: " + scopeType + ". Must be EQUIPMENT, DEPARTMENT, or INSTITUTION.");
        }

        return res;
    }

    private void verifyAnalyticsAccess(UserPrincipal caller) {
        Role role = caller.getRole();
        if (role == Role.RESEARCHER_STUDENT || role == Role.LAB_TECHNICIAN) {
            throw new UnauthorizedException("Researchers and Lab Technicians do not have access to utilization analytics.");
        }
    }

    private void verifyEquipmentScope(UserPrincipal caller, Equipment eq) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return;
        User user = userRepository.findById(caller.getId()).orElse(null);
        if (user == null) throw new UnauthorizedException("User not authenticated.");

        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            if (user.getDepartment() == null || !user.getDepartment().getId().equals(eq.getDepartment().getId())) {
                throw new UnauthorizedException("Department Heads can only access analytics for their department's equipment.");
            }
        } else {
            if (user.getInstitution() == null || !user.getInstitution().getId().equals(eq.getInstitution().getId())) {
                throw new UnauthorizedException("Access restricted to your institution's equipment.");
            }
        }
    }

    private void verifyDepartmentScope(UserPrincipal caller, Department dept) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return;
        User user = userRepository.findById(caller.getId()).orElse(null);
        if (user == null) throw new UnauthorizedException("User not authenticated.");

        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            if (user.getDepartment() == null || !user.getDepartment().getId().equals(dept.getId())) {
                throw new UnauthorizedException("Department Heads can only access their own department's analytics.");
            }
        } else {
            if (user.getInstitution() == null || !user.getInstitution().getId().equals(dept.getInstitution().getId())) {
                throw new UnauthorizedException("Access restricted to your institution's departments.");
            }
        }
    }

    private void verifyInstitutionScope(UserPrincipal caller, Institution inst) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return;
        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            throw new UnauthorizedException("Department Heads cannot access institution-wide analytics. Use department scope.");
        }
        User user = userRepository.findById(caller.getId()).orElse(null);
        if (user == null || user.getInstitution() == null || !user.getInstitution().getId().equals(inst.getId())) {
            throw new UnauthorizedException("Access restricted to your own institution.");
        }
    }

    private boolean hasAccessToEquipment(UserPrincipal caller, Equipment eq) {
        if (caller.getRole() == Role.SYSTEM_ADMIN) return true;
        User user = userRepository.findById(caller.getId()).orElse(null);
        if (user == null || user.getInstitution() == null) return false;
        if (!user.getInstitution().getId().equals(eq.getInstitution().getId())) return false;
        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            return user.getDepartment() != null && user.getDepartment().getId().equals(eq.getDepartment().getId());
        }
        return true;
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start date and end date are required.");
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }
        if (Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays() > 90) {
            throw new BadRequestException("Date range cannot exceed 90 days.");
        }
    }

    private double roundHours(long minutes) {
        return Math.round((minutes / 60.0) * 100.0) / 100.0;
    }

    private Map<Integer, EquipmentOperatingHours> getDefaultOperatingHoursMap(Equipment eq) {
        List<EquipmentOperatingHours> hours = operatingHoursService.getOperatingHours(eq.getId());
        return hours.stream().collect(Collectors.toMap(EquipmentOperatingHours::getDayOfWeek, h -> h, (a, b) -> a));
    }
}
