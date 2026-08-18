package com.disputecopilot.api.dto;

import java.util.UUID;

public record CreateDisputeRequest(UUID transactionId,
                                   String customerId,
                                   String reason,
                                   String reasonCode,
                                   String agentNotes) {
}
