package com.disputecopilot.api;

import com.disputecopilot.ai.DisputeAssistant;
import com.disputecopilot.api.dto.ChatRequest;
import com.disputecopilot.api.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final DisputeAssistant disputeAssistant;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(disputeAssistant.chat(request));
    }
}
