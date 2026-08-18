package com.disputecopilot.api;

import com.disputecopilot.api.dto.ChargebackCaseResponse;
import com.disputecopilot.api.dto.DisputeResponse;
import com.disputecopilot.api.dto.TransactionResponse;
import com.disputecopilot.domain.model.ChargebackCase;
import com.disputecopilot.domain.model.Dispute;
import com.disputecopilot.domain.model.Transaction;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static DisputeResponse toResponse(Dispute dispute) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getTransactionId(),
                dispute.getCustomerId(),
                dispute.getStatus().name(),
                dispute.getReason(),
                dispute.getReasonCode(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt(),
                dispute.getAgentNotes(),
                toResponse(dispute.getChargebackCase())
        );
    }

    public static ChargebackCaseResponse toResponse(ChargebackCase chargebackCase) {
        if (chargebackCase == null) {
            return null;
        }
        return new ChargebackCaseResponse(
                chargebackCase.getId(),
                chargebackCase.getVisaMastercardReasonCode(),
                chargebackCase.getEvidenceSummary(),
                chargebackCase.getFiledAt(),
                chargebackCase.isPendingConfirmation()
        );
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getMerchantName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTimestamp(),
                transaction.getStatus().name(),
                transaction.getScaStatus().name(),
                transaction.getThreeDsVersion()
        );
    }
}
