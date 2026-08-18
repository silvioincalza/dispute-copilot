package com.disputecopilot.api.dto;

public record ChatRequest(String sessionId, String message, String agentId) {
}
