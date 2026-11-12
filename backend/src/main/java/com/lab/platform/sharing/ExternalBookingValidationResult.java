package com.lab.platform.sharing;

import java.math.BigDecimal;

public class ExternalBookingValidationResult {

    private final Long sharingAgreementId;
    private final BigDecimal estimatedFee;

    public ExternalBookingValidationResult(Long sharingAgreementId, BigDecimal estimatedFee) {
        this.sharingAgreementId = sharingAgreementId;
        this.estimatedFee = estimatedFee;
    }

    public Long getSharingAgreementId() {
        return sharingAgreementId;
    }

    public BigDecimal getEstimatedFee() {
        return estimatedFee;
    }
}
