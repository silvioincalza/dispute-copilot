package com.disputecopilot.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyDocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PolicyDocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final DocumentSanitizer sanitizer;
    private final Map<String, Document> ingestedDocuments = new ConcurrentHashMap<>();

    public IngestionResult ingestBundledPolicies() {
        var resolver = new PathMatchingResourcePatternResolver();
        try {
            var resources = resolver.getResources("classpath*:policies/*.txt");
            var documentsToAdd = new ArrayList<Document>();
            var sources = new ArrayList<String>();
            for (Resource resource : resources) {
                var filename = resource.getFilename();
                if (filename == null || ingestedDocuments.containsKey(filename)) {
                    continue;
                }
                var text = sanitizer.sanitize(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                var metadata = new LinkedHashMap<String, Object>();
                metadata.put("source", filename);
                metadata.put("title", humanizeTitle(filename));
                metadata.put("documentType", "policy");
                metadata.put("filename", filename);
                var document = new Document(text, metadata);
                ingestedDocuments.put(filename, document);
                documentsToAdd.add(document);
                sources.add(filename);
            }
            if (!documentsToAdd.isEmpty()) {
                vectorStore.add(documentsToAdd);
                log.info("Ingested {} policy documents", documentsToAdd.size());
            }
            return new IngestionResult(documentsToAdd.size(), sources);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to ingest bundled policy documents", exception);
        }
    }

    public List<Document> currentDocuments() {
        return ingestedDocuments.values().stream()
                .sorted(Comparator.comparing(document -> String.valueOf(document.getMetadata().get("source"))))
                .toList();
    }

    private String humanizeTitle(String filename) {
        return filename.replace(".txt", "").replace('-', ' ');
    }

    public record IngestionResult(int ingestedCount, List<String> sources) {
    }
}
