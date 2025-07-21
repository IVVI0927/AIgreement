package com.example.legalai.llm.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class LlmService {

    private final WebClient webClient;

    public LlmService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public String analyzeContract(String content) {
        String prompt = String.format("""
            Read the following contract clause and return a JSON array of risky clauses. Each clause should have a "clause", "reason", and "risk level" field. Respond with JSON only, no explanation.

            Clause:
            %s
            """, content);

        Map<String, Object> body = Map.of(
            "model", "llama3",
            "prompt", prompt,
            "stream", false
        );

        return webClient.post()
            .uri("/api/generate")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .map(resp -> (String) resp.get("response"))
            .onErrorReturn("Failed to call LLaMA model")
            .block();
    }
} 