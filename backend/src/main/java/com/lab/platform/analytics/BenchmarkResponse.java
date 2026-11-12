package com.lab.platform.analytics;

import java.math.BigDecimal;

public class BenchmarkResponse {

    private BigDecimal currentUtilizationRate;
    private BigDecimal benchmarkUtilizationRate;
    private BigDecimal differencePercentagePoints;
    private String comparisonStatus; // "INCREASED", "DECREASED", "STABLE"

    public BenchmarkResponse() {}

    public BenchmarkResponse(BigDecimal currentUtilizationRate, BigDecimal benchmarkUtilizationRate,
                             BigDecimal differencePercentagePoints, String comparisonStatus) {
        this.currentUtilizationRate = currentUtilizationRate;
        this.benchmarkUtilizationRate = benchmarkUtilizationRate;
        this.differencePercentagePoints = differencePercentagePoints;
        this.comparisonStatus = comparisonStatus;
    }

    public BigDecimal getCurrentUtilizationRate() {
        return currentUtilizationRate;
    }

    public void setCurrentUtilizationRate(BigDecimal currentUtilizationRate) {
        this.currentUtilizationRate = currentUtilizationRate;
    }

    public BigDecimal getBenchmarkUtilizationRate() {
        return benchmarkUtilizationRate;
    }

    public void setBenchmarkUtilizationRate(BigDecimal benchmarkUtilizationRate) {
        this.benchmarkUtilizationRate = benchmarkUtilizationRate;
    }

    public BigDecimal getDifferencePercentagePoints() {
        return differencePercentagePoints;
    }

    public void setDifferencePercentagePoints(BigDecimal differencePercentagePoints) {
        this.differencePercentagePoints = differencePercentagePoints;
    }

    public String getComparisonStatus() {
        return comparisonStatus;
    }

    public void setComparisonStatus(String comparisonStatus) {
        this.comparisonStatus = comparisonStatus;
    }
}
