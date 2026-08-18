package com.disputecopilot.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ChargebackCaseResponse(UUID id,
                                     String visaMastercardReasonCode,
                                     String evidenceSummary,
                                     Instant filedAt,
                                     boolean pendingConfirmation) {
}
