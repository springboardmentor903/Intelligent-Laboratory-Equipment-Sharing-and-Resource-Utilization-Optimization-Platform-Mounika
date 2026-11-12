package com.lab.platform.sharing;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserPrincipal;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.booking.Booking;
import com.lab.platform.booking.BookingRepository;
import com.lab.platform.common.BadRequestException;
import com.lab.platform.common.ResourceNotFoundException;
import com.lab.platform.common.UnauthorizedException;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.institution.InstitutionRepository;
import com.lab.platform.usage.UsageLog;
import com.lab.platform.usage.UsageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SharingAnalyticsService {

    private final SharingAgreementRepository agreementRepository;
    private final AccessGrantRepository grantRepository;
    private final SharingListingRepository listingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;

    public SharingAnalyticsService(
            SharingAgreementRepository agreementRepository,
            AccessGrantRepository grantRepository,
            SharingListingRepository listingRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            InstitutionRepository institutionRepository,
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository) {
        this.agreementRepository = agreementRepository;
        this.grantRepository = grantRepository;
        this.listingRepository = listingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
    }

    public SharingAnalyticsSummaryDto getSummary(UserPrincipal caller) {
        User user = getUser(caller.getId());
        Long callerInstId = (caller.getRole() == Role.SYSTEM_ADMIN) ? null : getInstitutionId(user);

        List<SharingAgreement> agreements = (callerInstId == null)
                ? agreementRepository.findAll()
                : agreementRepository.findByInstitutionId(callerInstId);

        List<UsageLog> allCompletedLogs = usageLogRepository.findAll().stream()
                .filter(l -> l.getActualEndTime() != null && l.getBooking() != null && l.getBooking().getSharingAgreementId() != null)
                .collect(Collectors.toList());

        long providedMinutes = 0;
        long usedMinutes = 0;
        BigDecimal feesEarned = BigDecimal.ZERO;
        BigDecimal feesOwed = BigDecimal.ZERO;

        Map<Long, SharingAgreement> agreementMap = agreements.stream()
                .collect(Collectors.toMap(SharingAgreement::getId, a -> a, (a, b) -> a));

        for (UsageLog log : allCompletedLogs) {
            SharingAgreement agr = agreementMap.get(log.getBooking().getSharingAgreementId());
            if (agr == null) continue;

            long duration = log.calculateDurationMinutes(LocalDateTime.now());
            BigDecimal fee = (log.getFeeCharged() != null)
                    ? log.getFeeCharged()
                    : agr.getFeePerHour().multiply(BigDecimal.valueOf(duration))
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            Long providerInstId = agr.getProviderInstitution().getId();
            Long consumerInstId = agr.getConsumerInstitution().getId();

            if (callerInstId == null || callerInstId.equals(providerInstId)) {
                providedMinutes += duration;
                feesEarned = feesEarned.add(fee);
            }

            if (callerInstId != null && callerInstId.equals(consumerInstId)) {
                usedMinutes += duration;
                feesOwed = feesOwed.add(fee);
            }
        }

        Set<Long> partnerInstitutionIds = new HashSet<>();
        int activeAgreements = 0;
        int expiringAgreements = 0;
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        for (SharingAgreement a : agreements) {
            if (a.getStatus() == SharingAgreementStatus.ACTIVE) {
                activeAgreements++;
                if (a.getValidTo() != null && !a.getValidTo().isBefore(today) && !a.getValidTo().isAfter(thirtyDaysFromNow)) {
                    expiringAgreements++;
                }
                if (callerInstId != null) {
                    if (a.getProviderInstitution().getId().equals(callerInstId)) {
                        partnerInstitutionIds.add(a.getConsumerInstitution().getId());
                    } else if (a.getConsumerInstitution().getId().equals(callerInstId)) {
                        partnerInstitutionIds.add(a.getProviderInstitution().getId());
                    }
                } else {
                    partnerInstitutionIds.add(a.getProviderInstitution().getId());
                    partnerInstitutionIds.add(a.getConsumerInstitution().getId());
                }
            }
        }

        SharingAnalyticsSummaryDto dto = new SharingAnalyticsSummaryDto();
        dto.setHoursProvided(providedMinutes / 60);
        dto.setHoursUsed(usedMinutes / 60);
        dto.setFeesEarned(feesEarned);
        dto.setFeesOwed(feesOwed);
        dto.setNetFinancialBalance(feesEarned.subtract(feesOwed));
        dto.setActivePartnersCount(partnerInstitutionIds.size());
        dto.setActiveAgreementsCount(activeAgreements);
        dto.setExpiringAgreementsCount(expiringAgreements);

        long totalInstitutionUsageMinutes = usageLogRepository.findAll().stream()
                .filter(l -> l.getActualEndTime() != null)
                .filter(l -> callerInstId == null || (l.getEquipment().getDepartment() != null
                        && l.getEquipment().getDepartment().getInstitution() != null
                        && callerInstId.equals(l.getEquipment().getDepartment().getInstitution().getId())))
                .mapToLong(l -> l.calculateDurationMinutes(LocalDateTime.now()))
                .sum();

        double externalSharePct = (totalInstitutionUsageMinutes > 0)
                ? ((double) providedMinutes / totalInstitutionUsageMinutes) * 100.0
                : 0.0;
        dto.setExternalSharePercentage(Math.round(externalSharePct * 10.0) / 10.0);

        return dto;
    }

    public List<PartnerSummaryDto> getPartners(UserPrincipal caller) {
        User user = getUser(caller.getId());
        Long callerInstId = (caller.getRole() == Role.SYSTEM_ADMIN) ? null : getInstitutionId(user);

        List<SharingAgreement> agreements = (callerInstId == null)
                ? agreementRepository.findAll()
                : agreementRepository.findByInstitutionId(callerInstId);

        Map<Long, PartnerSummaryDto> partnerMap = new LinkedHashMap<>();

        for (SharingAgreement a : agreements) {
            final Institution partnerInst = (callerInstId == null)
                    ? a.getConsumerInstitution()
                    : (a.getProviderInstitution().getId().equals(callerInstId)
                            ? a.getConsumerInstitution()
                            : a.getProviderInstitution());

            final Long partnerId = partnerInst.getId();
            final String partnerName = partnerInst.getName();
            final String agrNum = a.getAgreementNumber();
            final LocalDate validTo = a.getValidTo();
            final String statusName = a.getStatus().name();

            PartnerSummaryDto summary = partnerMap.computeIfAbsent(partnerId, pid -> {
                PartnerSummaryDto p = new PartnerSummaryDto();
                p.setPartnerInstitutionId(partnerId);
                p.setPartnerInstitutionName(partnerName);
                p.setAgreementNumber(agrNum);
                p.setAgreementValidUntil(validTo);
                p.setAgreementStatus(statusName);
                return p;
            });

            // Grants count
            List<AccessGrant> grants = grantRepository.findByAgreementId(a.getId());
            summary.setActiveGrantsCount(summary.getActiveGrantsCount() + (int) grants.stream().filter(AccessGrant::isActive).count());

            // Compute usage for this agreement
            List<UsageLog> logs = usageLogRepository.findAll().stream()
                    .filter(l -> l.getActualEndTime() != null && l.getBooking() != null && a.getId().equals(l.getBooking().getSharingAgreementId()))
                    .collect(Collectors.toList());

            for (UsageLog log : logs) {
                long duration = log.calculateDurationMinutes(LocalDateTime.now());
                BigDecimal fee = (log.getFeeCharged() != null)
                        ? log.getFeeCharged()
                        : a.getFeePerHour().multiply(BigDecimal.valueOf(duration))
                                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

                boolean callerIsProvider = (callerInstId == null || a.getProviderInstitution().getId().equals(callerInstId));
                if (callerIsProvider) {
                    summary.setHoursProvidedToPartner(summary.getHoursProvidedToPartner() + (duration / 60));
                    summary.setFeesEarnedFromPartner(summary.getFeesEarnedFromPartner().add(fee));
                } else {
                    summary.setHoursUsedFromPartner(summary.getHoursUsedFromPartner() + (duration / 60));
                    summary.setFeesOwedToPartner(summary.getFeesOwedToPartner().add(fee));
                }
            }
            summary.setNetBalance(summary.getFeesEarnedFromPartner().subtract(summary.getFeesOwedToPartner()));
        }

        return new ArrayList<>(partnerMap.values());
    }

    public List<SharingTrendPointDto> getTrends(UserPrincipal caller, int months) {
        User user = getUser(caller.getId());
        Long callerInstId = (caller.getRole() == Role.SYSTEM_ADMIN) ? null : getInstitutionId(user);

        int horizonMonths = (months > 0 && months <= 12) ? months : 6;
        LocalDate startMonth = LocalDate.now().minusMonths(horizonMonths - 1).withDayOfMonth(1);

        Map<String, SharingTrendPointDto> trendMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        LocalDate cur = startMonth;
        while (!cur.isAfter(LocalDate.now())) {
            String key = cur.format(formatter);
            trendMap.put(key, new SharingTrendPointDto(key, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO));
            cur = cur.plusMonths(1);
        }

        List<UsageLog> sharedLogs = usageLogRepository.findAll().stream()
                .filter(l -> l.getActualEndTime() != null && l.getBooking() != null && l.getBooking().getSharingAgreementId() != null)
                .collect(Collectors.toList());

        for (UsageLog log : sharedLogs) {
            LocalDate logDate = log.getActualStartTime().toLocalDate();
            if (logDate.isBefore(startMonth)) continue;

            String key = logDate.format(formatter);
            SharingTrendPointDto point = trendMap.get(key);
            if (point == null) continue;

            SharingAgreement agr = agreementRepository.findById(log.getBooking().getSharingAgreementId()).orElse(null);
            if (agr == null) continue;

            long duration = log.calculateDurationMinutes(LocalDateTime.now());
            BigDecimal fee = (log.getFeeCharged() != null)
                    ? log.getFeeCharged()
                    : agr.getFeePerHour().multiply(BigDecimal.valueOf(duration))
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            boolean callerIsProvider = (callerInstId == null || agr.getProviderInstitution().getId().equals(callerInstId));
            boolean callerIsConsumer = (callerInstId != null && agr.getConsumerInstitution().getId().equals(callerInstId));

            if (callerIsProvider) {
                point.setHoursProvided(point.getHoursProvided() + (duration / 60));
                point.setFeesEarned(point.getFeesEarned().add(fee));
            }
            if (callerIsConsumer) {
                point.setHoursUsed(point.getHoursUsed() + (duration / 60));
                point.setFeesOwed(point.getFeesOwed().add(fee));
            }
        }

        return new ArrayList<>(trendMap.values());
    }

    public List<MostSharedEquipmentDto> getMostSharedEquipment(UserPrincipal caller, int limit) {
        User user = getUser(caller.getId());
        Long callerInstId = (caller.getRole() == Role.SYSTEM_ADMIN) ? null : getInstitutionId(user);

        int maxResults = (limit > 0 && limit <= 50) ? limit : 10;

        List<Equipment> equipmentList = (callerInstId == null)
                ? equipmentRepository.findAll()
                : equipmentRepository.findByInstitutionId(callerInstId);

        List<MostSharedEquipmentDto> result = new ArrayList<>();

        for (Equipment eq : equipmentList) {
            List<UsageLog> sharedLogs = usageLogRepository.findByEquipmentId(eq.getId()).stream()
                    .filter(l -> l.getActualEndTime() != null && l.getBooking() != null && l.getBooking().getSharingAgreementId() != null)
                    .collect(Collectors.toList());

            if (sharedLogs.isEmpty()) continue;

            long externalMinutes = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            for (UsageLog l : sharedLogs) {
                long dur = l.calculateDurationMinutes(LocalDateTime.now());
                externalMinutes += dur;
                if (l.getFeeCharged() != null) {
                    revenue = revenue.add(l.getFeeCharged());
                } else if (eq.getHourlyRate() != null) {
                    revenue = revenue.add(eq.getHourlyRate().multiply(BigDecimal.valueOf(dur))
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
                }
            }

            MostSharedEquipmentDto dto = new MostSharedEquipmentDto();
            dto.setEquipmentId(eq.getId());
            dto.setEquipmentName(eq.getName());
            dto.setCategory(eq.getCategory());
            dto.setDepartmentName(eq.getDepartment() != null ? eq.getDepartment().getName() : "General Lab");
            dto.setExternalHoursUsed(externalMinutes / 60);
            dto.setExternalBookingsCount(sharedLogs.size());
            dto.setRevenueGenerated(revenue);

            long totalEqMinutes = usageLogRepository.findByEquipmentId(eq.getId()).stream()
                    .filter(l -> l.getActualEndTime() != null)
                    .mapToLong(l -> l.calculateDurationMinutes(LocalDateTime.now()))
                    .sum();
            double shareRate = (totalEqMinutes > 0) ? ((double) externalMinutes / totalEqMinutes) * 100.0 : 0.0;
            dto.setExternalUtilizationRate(Math.round(shareRate * 10.0) / 10.0);

            result.add(dto);
        }

        result.sort((a, b) -> Long.compare(b.getExternalHoursUsed(), a.getExternalHoursUsed()));
        return result.stream().limit(maxResults).collect(Collectors.toList());
    }

    public PartnershipReportDto getPartnershipReport(Long partnerInstitutionId, UserPrincipal caller) {
        User user = getUser(caller.getId());
        Long callerInstId = (caller.getRole() == Role.SYSTEM_ADMIN) ? null : getInstitutionId(user);

        Institution partnerInst = institutionRepository.findById(partnerInstitutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner institution not found with id: " + partnerInstitutionId));

        PartnershipReportDto report = new PartnershipReportDto();

        // 1. Partner summary
        List<PartnerSummaryDto> partners = getPartners(caller);
        PartnerSummaryDto pSummary = partners.stream()
                .filter(p -> p.getPartnerInstitutionId().equals(partnerInstitutionId))
                .findFirst()
                .orElseGet(() -> {
                    PartnerSummaryDto fallback = new PartnerSummaryDto();
                    fallback.setPartnerInstitutionId(partnerInstitutionId);
                    fallback.setPartnerInstitutionName(partnerInst.getName());
                    fallback.setAgreementStatus("ACTIVE");
                    return fallback;
                });
        report.setPartner(pSummary);

        // 2. Find agreements between caller and partner
        List<SharingAgreement> bilateralAgreements = agreementRepository.findAll().stream()
                .filter(a -> {
                    if (callerInstId == null) {
                        return a.getProviderInstitution().getId().equals(partnerInstitutionId)
                                || a.getConsumerInstitution().getId().equals(partnerInstitutionId);
                    }
                    return (a.getProviderInstitution().getId().equals(callerInstId) && a.getConsumerInstitution().getId().equals(partnerInstitutionId))
                            || (a.getProviderInstitution().getId().equals(partnerInstitutionId) && a.getConsumerInstitution().getId().equals(callerInstId));
                })
                .collect(Collectors.toList());

        Set<Long> bilateralAgreementIds = bilateralAgreements.stream().map(SharingAgreement::getId).collect(Collectors.toSet());

        // 3. Shared sessions
        List<ItemizedFeeItemDto> sessions = new ArrayList<>();
        List<UsageLog> bilateralLogs = usageLogRepository.findAll().stream()
                .filter(l -> l.getActualEndTime() != null && l.getBooking() != null && bilateralAgreementIds.contains(l.getBooking().getSharingAgreementId()))
                .collect(Collectors.toList());

        for (UsageLog log : bilateralLogs) {
            ItemizedFeeItemDto item = new ItemizedFeeItemDto();
            item.setBookingId(log.getBooking().getId());
            item.setUsageLogId(log.getId());
            item.setEquipmentName(log.getEquipment().getName());
            item.setResearcherName(log.getUser().getFullName());
            item.setConsumerInstitutionName(partnerInst.getName());
            item.setSessionStartTime(log.getActualStartTime());
            item.setSessionEndTime(log.getActualEndTime());
            long dur = log.calculateDurationMinutes(LocalDateTime.now());
            item.setDurationMinutes(dur);
            item.setFeeCharged(log.getFeeCharged() != null ? log.getFeeCharged() : BigDecimal.ZERO);
            sessions.add(item);
        }
        report.setSharedSessions(sessions);

        // 4. Equipment provided to partner
        Map<Long, MostSharedEquipmentDto> providedMap = new HashMap<>();
        Map<Long, MostSharedEquipmentDto> consumedMap = new HashMap<>();

        for (UsageLog log : bilateralLogs) {
            Equipment eq = log.getEquipment();
            Long eqInstId = (eq.getDepartment() != null && eq.getDepartment().getInstitution() != null)
                    ? eq.getDepartment().getInstitution().getId() : null;

            boolean isProvided = (callerInstId == null || (eqInstId != null && eqInstId.equals(callerInstId)));
            Map<Long, MostSharedEquipmentDto> targetMap = isProvided ? providedMap : consumedMap;

            MostSharedEquipmentDto item = targetMap.computeIfAbsent(eq.getId(), k -> {
                MostSharedEquipmentDto m = new MostSharedEquipmentDto();
                m.setEquipmentId(eq.getId());
                m.setEquipmentName(eq.getName());
                m.setCategory(eq.getCategory());
                m.setDepartmentName(eq.getDepartment() != null ? eq.getDepartment().getName() : "General Lab");
                return m;
            });

            long dur = log.calculateDurationMinutes(LocalDateTime.now());
            item.setExternalHoursUsed(item.getExternalHoursUsed() + (dur / 60));
            item.setExternalBookingsCount(item.getExternalBookingsCount() + 1);
            if (log.getFeeCharged() != null) {
                item.setRevenueGenerated(item.getRevenueGenerated().add(log.getFeeCharged()));
            }
        }

        report.setEquipmentProvided(new ArrayList<>(providedMap.values()));
        report.setEquipmentConsumed(new ArrayList<>(consumedMap.values()));

        return report;
    }

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
}
