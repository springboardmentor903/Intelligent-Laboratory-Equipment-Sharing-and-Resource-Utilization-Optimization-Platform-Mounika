package com.lab.platform.sharing;

import java.math.BigDecimal;

public class SharingAnalyticsSummaryDto {

    private long hoursProvided;
    private long hoursUsed;
    private BigDecimal feesEarned = BigDecimal.ZERO;
    private BigDecimal feesOwed = BigDecimal.ZERO;
    private BigDecimal netFinancialBalance = BigDecimal.ZERO;
    private int activePartnersCount;
    private int activeAgreementsCount;
    private int expiringAgreementsCount;
    private double externalSharePercentage; // External hours / Total institution hours * 100

    public SharingAnalyticsSummaryDto() {}

    public long getHoursProvided() {
        return hoursProvided;
    }

    public void setHoursProvided(long hoursProvided) {
        this.hoursProvided = hoursProvided;
    }

    public long getHoursUsed() {
        return hoursUsed;
    }

    public void setHoursUsed(long hoursUsed) {
        this.hoursUsed = hoursUsed;
    }

    public BigDecimal getFeesEarned() {
        return feesEarned;
    }

    public void setFeesEarned(BigDecimal feesEarned) {
        this.feesEarned = feesEarned;
    }

    public BigDecimal getFeesOwed() {
        return feesOwed;
    }

    public void setFeesOwed(BigDecimal feesOwed) {
        this.feesOwed = feesOwed;
    }

    public BigDecimal getNetFinancialBalance() {
        return netFinancialBalance;
    }

    public void setNetFinancialBalance(BigDecimal netFinancialBalance) {
        this.netFinancialBalance = netFinancialBalance;
    }

    public int getActivePartnersCount() {
        return activePartnersCount;
    }

    public void setActivePartnersCount(int activePartnersCount) {
        this.activePartnersCount = activePartnersCount;
    }

    public int getActiveAgreementsCount() {
        return activeAgreementsCount;
    }

    public void setActiveAgreementsCount(int activeAgreementsCount) {
        this.activeAgreementsCount = activeAgreementsCount;
    }

    public int getExpiringAgreementsCount() {
        return expiringAgreementsCount;
    }

    public void setExpiringAgreementsCount(int expiringAgreementsCount) {
        this.expiringAgreementsCount = expiringAgreementsCount;
    }

    public double getExternalSharePercentage() {
        return externalSharePercentage;
    }

    public void setExternalSharePercentage(double externalSharePercentage) {
        this.externalSharePercentage = externalSharePercentage;
    }
}
