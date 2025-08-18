package com.example.legalai.client;

import com.example.legalai.dto.ContractAnalysisRequest;
import com.example.legalai.dto.ContractAnalysisResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "llm-service",
    fallback = LlmServiceClientFallback.class,
    configuration = FeignClientConfiguration.class
)
public interface LlmServiceClient {
    
    @PostMapping("/api/llm/analyze")
    ContractAnalysisResponse analyzeContract(
        @RequestBody ContractAnalysisRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId,
        @RequestHeader("X-Service-Auth") String serviceAuth
    );
    
    @PostMapping("/api/llm/extract-clauses")
    ContractAnalysisResponse extractClauses(
        @RequestBody ContractAnalysisRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId,
        @RequestHeader("X-Service-Auth") String serviceAuth
    );
    
    @PostMapping("/api/llm/risk-assessment")
    ContractAnalysisResponse assessRisk(
        @RequestBody ContractAnalysisRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId,
        @RequestHeader("X-Service-Auth") String serviceAuth
    );
}