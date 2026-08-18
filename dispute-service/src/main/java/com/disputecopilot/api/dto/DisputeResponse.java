package com.disputecopilot.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DisputeResponse(UUID id,
                              UUID transactionId,
                              String customerId,
                              String status,
                              String reason,
                              String reasonCode,
                              Instant createdAt,
                              Instant updatedAt,
                              String agentNotes,
                              ChargebackCaseResponse chargebackCase) {
}
