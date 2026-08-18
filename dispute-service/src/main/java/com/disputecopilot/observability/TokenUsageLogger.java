package com.disputecopilot.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenUsageLogger {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageLogger.class);

    public void log(String sessionId, ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            log.info("sessionId={} promptTokens=unknown completionTokens=unknown totalTokens=unknown", sessionId);
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        log.info("sessionId={} promptTokens={} completionTokens={} totalTokens={}",
                sessionId,
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());
    }
}
