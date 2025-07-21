package com.example.legalai.llm.controller;

import com.example.legalai.llm.dto.AnalysisRequest;
import com.example.legalai.llm.dto.AnalysisResponse;
import com.example.legalai.llm.service.LlmAnalysisService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LlmController {

    private final LlmAnalysisService llmAnalysisService;

    @PostMapping("/analyze")
    @CircuitBreaker(name = "llmAnalysis", fallbackMethod = "analyzeFallback")
    public Mono<ResponseEntity<AnalysisResponse>> analyzeContract(@RequestBody AnalysisRequest request) {
        return llmAnalysisService.analyzeContract(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError()
                        .body(AnalysisResponse.builder()
                                .error("LLM service temporarily unavailable")
                                .build()));
    }

    @PostMapping("/analyze/async")
    public ResponseEntity<String> analyzeContractAsync(@RequestBody AnalysisRequest request) {
        String taskId = llmAnalysisService.analyzeContractAsync(request);
        return ResponseEntity.ok(taskId);
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<AnalysisResponse> getAnalysisStatus(@PathVariable String taskId) {
        return llmAnalysisService.getAnalysisStatus(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<AnalysisResponse> analyzeFallback(AnalysisRequest request, Exception e) {
        return ResponseEntity.ok(AnalysisResponse.builder()
                .error("Service temporarily unavailable. Please try again later.")
                .build());
    }
} 