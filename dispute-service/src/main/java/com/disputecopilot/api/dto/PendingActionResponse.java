package com.disputecopilot.api.dto;

import java.util.UUID;

public record PendingActionResponse(String type, UUID resourceId, String description, boolean requiresConfirmation) {
}
