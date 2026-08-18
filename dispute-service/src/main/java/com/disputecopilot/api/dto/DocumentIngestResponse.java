package com.disputecopilot.api.dto;

import java.util.List;

public record DocumentIngestResponse(int ingestedCount, List<String> sources) {
}
