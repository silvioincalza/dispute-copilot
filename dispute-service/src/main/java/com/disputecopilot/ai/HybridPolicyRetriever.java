package com.disputecopilot.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HybridPolicyRetriever {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of("the", "a", "an", "for", "to", "and", "or", "of", "in", "on", "with", "show", "me");

    private final VectorStore vectorStore;
    private final PolicyDocumentIngestionService policyDocumentIngestionService;

    public List<RetrievedPolicyDocument> retrieve(String query, int limit) {
        var combined = new LinkedHashMap<String, RetrievedPolicyDocument>();

        for (var document : vectorResults(query, limit)) {
            combined.put(source(document), toRetrieved(document));
        }

        for (var document : keywordResults(query, limit)) {
            combined.putIfAbsent(source(document), toRetrieved(document));
        }

        return combined.values().stream().limit(limit).toList();
    }

    private List<Document> vectorResults(String query, int limit) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(limit).build());
    }

    private List<Document> keywordResults(String query, int limit) {
        var tokens = TOKEN_SPLIT.splitAsStream(query.toLowerCase(Locale.ROOT))
                .filter(token -> !token.isBlank())
                .filter(token -> !STOP_WORDS.contains(token))
                .toList();
        if (tokens.isEmpty()) {
            return List.of();
        }

        var scored = new ArrayList<Map.Entry<Document, Integer>>();
        for (var document : policyDocumentIngestionService.currentDocuments()) {
            var haystack = (document.getText() + " " + title(document)).toLowerCase(Locale.ROOT);
            int score = 0;
            for (var token : tokens) {
                if (haystack.contains(token)) {
                    score++;
                }
            }
            if (score > 0) {
                scored.add(Map.entry(document, score));
            }
        }

        return scored.stream()
                .sorted(Map.Entry.<Document, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private RetrievedPolicyDocument toRetrieved(Document document) {
        var text = document.getText();
        return new RetrievedPolicyDocument(source(document), title(document), text.length() > 220 ? text.substring(0, 220) + "..." : text);
    }

    private String source(Document document) {
        return String.valueOf(document.getMetadata().getOrDefault("source", "unknown"));
    }

    private String title(Document document) {
        return String.valueOf(document.getMetadata().getOrDefault("title", source(document)));
    }

    public record RetrievedPolicyDocument(String source, String title, String excerpt) {
    }
}
