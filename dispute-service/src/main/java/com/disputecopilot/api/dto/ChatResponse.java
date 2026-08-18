package com.disputecopilot.api.dto;

import java.util.List;

public record ChatResponse(String sessionId,
                           String response,
                           List<CitationResponse> citations,
                           List<PendingActionResponse> pendingActions) {
}
