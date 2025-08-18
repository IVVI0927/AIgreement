package com.example.legalai.client;

import com.example.legalai.dto.ContractAnalysisRequest;
import com.example.legalai.dto.ContractAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

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
        ContractAnalysisResponse response = new ContractAnalysisResponse();
        response.setStatus("FALLBACK");
        response.setSummary(message);
        response.setRiskLevel("UNKNOWN");
        response.setKeyClauses(Collections.emptyList());
        response.setIssues(Collections.singletonList(message));
        response.setRecommendations(Collections.singletonList(
            "Please try again later or contact support if the issue persists"
        ));
        return response;
    }
}