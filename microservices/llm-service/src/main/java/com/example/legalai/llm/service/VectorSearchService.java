package com.example.legalai.llm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Service
public class VectorSearchService {

    private final WebClient chromaClient;
    private final LlmService llmService;
    
    @Value("${chroma.collection:legal-documents}")
    private String collectionName;

    public VectorSearchService(LlmService llmService) {
        this.llmService = llmService;
        this.chromaClient = WebClient.builder()
            .baseUrl("http://localhost:8000")
            .build();
    }

    public Mono<String> addDocument(String documentId, String content, Map<String, String> metadata) {
        Map<String, Object> request = new HashMap<>();
        request.put("ids", List.of(documentId));
        request.put("documents", List.of(content));
        request.put("metadatas", List.of(metadata));
        
        return chromaClient.post()
            .uri("/api/v1/collections/" + collectionName + "/add")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(result -> log.info("Document added to vector store: {}", documentId))
            .doOnError(error -> log.error("Failed to add document to vector store", error));
    }

    public Mono<List<SearchResult>> semanticSearch(String query, int topK) {
        Map<String, Object> request = new HashMap<>();
        request.put("query_texts", List.of(query));
        request.put("n_results", topK);
        
        return chromaClient.post()
            .uri("/api/v1/collections/" + collectionName + "/query")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(this::parseSearchResults)
            .doOnSuccess(results -> log.info("Semantic search completed, found {} results", results.size()))
            .doOnError(error -> log.error("Semantic search failed", error));
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> parseSearchResults(Map<String, Object> response) {
        List<SearchResult> results = new ArrayList<>();
        
        List<List<String>> ids = (List<List<String>>) response.get("ids");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");
        List<List<Double>> distances = (List<List<Double>>) response.get("distances");
        List<List<String>> documents = (List<List<String>>) response.get("documents");
        
        if (ids != null && !ids.isEmpty()) {
            List<String> idList = ids.get(0);
            List<Map<String, Object>> metadataList = metadatas != null ? metadatas.get(0) : new ArrayList<>();
            List<Double> distanceList = distances != null ? distances.get(0) : new ArrayList<>();
            List<String> documentList = documents != null ? documents.get(0) : new ArrayList<>();
            
            for (int i = 0; i < idList.size(); i++) {
                SearchResult result = new SearchResult();
                result.setId(idList.get(i));
                result.setContent(i < documentList.size() ? documentList.get(i) : "");
                result.setScore(i < distanceList.size() ? 1.0 - distanceList.get(i) : 0.0);
                result.setMetadata(i < metadataList.size() ? metadataList.get(i) : new HashMap<>());
                results.add(result);
            }
        }
        
        return results;
    }

    public Mono<String> generateRAGResponse(String query, List<SearchResult> context) {
        StringBuilder contextBuilder = new StringBuilder();
        for (SearchResult result : context) {
            contextBuilder.append("Document: ").append(result.getContent()).append("\n\n");
        }
        
        String prompt = String.format("""
            Based on the following legal documents context, answer the question.
            
            Context:
            %s
            
            Question: %s
            
            Provide a comprehensive answer based on the context provided.
            """, contextBuilder.toString(), query);
        
        return Mono.just(llmService.analyzeContract(prompt));
    }

    public static class SearchResult {
        private String id;
        private String content;
        private double score;
        private Map<String, Object> metadata;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}