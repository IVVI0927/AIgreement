package com.example.legalai.client;

import com.example.legalai.dto.ContractAnalysisRequest;
import com.example.legalai.dto.ContractAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmServiceClientFallback implements LlmServiceClient {
    
    @Override
    public ContractAnalysisResponse analyzeContract(
            ContractAnalysisRequest request, 
            String correlationId, 
            String serviceAuth) {
        log.warn("Fallback triggered for analyzeContract - correlationId: {}", correlationId);
        return createFallbackResponse("LLM service is temporarily unavailable for contract analysis");
    }
    
    @Override
    public ContractAnalysisResponse extractClauses(
            ContractAnalysisRequest request, 
            String correlationId, 
            String serviceAuth) {
        log.warn("Fallback triggered for extractClauses - correlationId: {}", correlationId);
        return createFallbackResponse("LLM service is temporarily unavailable for clause extraction");
    }
    
    @Override
    public ContractAnalysisResponse assessRisk(
            ContractAnalysisRequest request, 
            String correlationId, 
            String serviceAuth) {
        log.warn("Fallback triggered for assessRisk - correlationId: {}", correlationId);
        return createFallbackResponse("LLM service is temporarily unavailable for risk assessment");
    }
    
    private ContractAnalysisResponse createFallbackResponse(String message) {
        return ContractAnalysisResponse.builder()
                .contractTitle("N/A")
                .analyzedAt(LocalDateTime.now())
                .analysisResults(List.of(Map.of(
                        "status", "FALLBACK",
                        "summary", message,
                        "riskLevel", "UNKNOWN",
                        "recommendation", "Please try again later or contact support"
                )))
                .build();
    }
}
