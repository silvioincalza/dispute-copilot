package com.disputecopilot.ai;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DocumentSanitizer {

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+all\\s+previous\\s+instructions"),
            Pattern.compile("(?i)ignore\\s+previous\\s+instructions"),
            Pattern.compile("(?i)system\\s+prompt"),
            Pattern.compile("(?i)developer\\s+message"),
            Pattern.compile("(?i)tool\\s*:")
    );

    public String sanitize(String text) {
        var sanitized = text == null ? "" : text;
        for (var pattern : DANGEROUS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[redacted-instruction]");
        }
        sanitized = sanitized.replaceAll("(?is)```.*?```", " ");
        sanitized = sanitized.replaceAll("[\\u0000-\\u001F]", " ");
        return sanitized.replaceAll("\\s+", " ").trim();
    }
}
