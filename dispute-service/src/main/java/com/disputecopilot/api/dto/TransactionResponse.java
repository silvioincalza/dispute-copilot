package com.disputecopilot.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id,
                                  String customerId,
                                  String merchantName,
                                  BigDecimal amount,
                                  String currency,
                                  Instant timestamp,
                                  String status,
                                  String scaStatus,
                                  String threeDsVersion) {
}
