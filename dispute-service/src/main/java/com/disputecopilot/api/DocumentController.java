package com.disputecopilot.api;

import com.disputecopilot.ai.PolicyDocumentIngestionService;
import com.disputecopilot.api.dto.DocumentIngestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final PolicyDocumentIngestionService ingestionService;

    @PostMapping("/ingest")
    public ResponseEntity<DocumentIngestResponse> ingestDocuments() {
        var result = ingestionService.ingestBundledPolicies();
        return ResponseEntity.ok(new DocumentIngestResponse(result.ingestedCount(), result.sources()));
    }
}
