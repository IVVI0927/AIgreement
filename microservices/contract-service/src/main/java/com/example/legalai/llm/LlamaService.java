package com.example.legalAI.llm;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class LlamaService {

    private final WebClient webClient;

    public LlamaService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public String sendPrompt(String prompt) {
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
