package com.disputecopilot.api.dto;

import java.util.UUID;

public record ScaDetailsResponse(UUID transactionId,
                                 String scaStatus,
                                 String threeDsVersion,
                                 String transactionStatus) {
}
