package com.lab.platform.sharing;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PartnerSummaryDto {

    private Long partnerInstitutionId;
    private String partnerInstitutionName;
    private String agreementNumber;
    private LocalDate agreementValidUntil;
    private String agreementStatus;
    private long hoursProvidedToPartner;
    private long hoursUsedFromPartner;
    private BigDecimal feesEarnedFromPartner = BigDecimal.ZERO;
    private BigDecimal feesOwedToPartner = BigDecimal.ZERO;
    private BigDecimal netBalance = BigDecimal.ZERO; // earned - owed
    private int activeGrantsCount;

    public PartnerSummaryDto() {}

    public Long getPartnerInstitutionId() {
        return partnerInstitutionId;
    }

    public void setPartnerInstitutionId(Long partnerInstitutionId) {
        this.partnerInstitutionId = partnerInstitutionId;
    }

    public String getPartnerInstitutionName() {
        return partnerInstitutionName;
    }

    public void setPartnerInstitutionName(String partnerInstitutionName) {
        this.partnerInstitutionName = partnerInstitutionName;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public LocalDate getAgreementValidUntil() {
        return agreementValidUntil;
    }

    public void setAgreementValidUntil(LocalDate agreementValidUntil) {
        this.agreementValidUntil = agreementValidUntil;
    }

    public String getAgreementStatus() {
        return agreementStatus;
    }

    public void setAgreementStatus(String agreementStatus) {
        this.agreementStatus = agreementStatus;
    }

    public long getHoursProvidedToPartner() {
        return hoursProvidedToPartner;
    }

    public void setHoursProvidedToPartner(long hoursProvidedToPartner) {
        this.hoursProvidedToPartner = hoursProvidedToPartner;
    }

    public long getHoursUsedFromPartner() {
        return hoursUsedFromPartner;
    }

    public void setHoursUsedFromPartner(long hoursUsedFromPartner) {
        this.hoursUsedFromPartner = hoursUsedFromPartner;
    }

    public BigDecimal getFeesEarnedFromPartner() {
        return feesEarnedFromPartner;
    }

    public void setFeesEarnedFromPartner(BigDecimal feesEarnedFromPartner) {
        this.feesEarnedFromPartner = feesEarnedFromPartner;
    }

    public BigDecimal getFeesOwedToPartner() {
        return feesOwedToPartner;
    }

    public void setFeesOwedToPartner(BigDecimal feesOwedToPartner) {
        this.feesOwedToPartner = feesOwedToPartner;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public int getActiveGrantsCount() {
        return activeGrantsCount;
    }

    public void setActiveGrantsCount(int activeGrantsCount) {
        this.activeGrantsCount = activeGrantsCount;
    }
}
