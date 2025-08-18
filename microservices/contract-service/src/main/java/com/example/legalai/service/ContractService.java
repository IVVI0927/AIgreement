package com.example.legalai.service;

import com.example.legalai.client.LlmServiceClient;
import com.example.legalai.dto.ContractAnalysisRequest;
import com.example.legalai.dto.ContractAnalysisResponse;
import com.example.legalai.model.ContractDocument;
import com.example.legalai.llm.LlamaService;
import com.example.legalai.repository.ContractDocumentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContractService {

    private final LlamaService llamaService;
    private final ContractDocumentRepository contractRepo;
    private final LlmServiceClient llmServiceClient;
    private static final String SERVICE_AUTH_KEY = "internal-service-key";

    @Autowired
    public ContractService(LlamaService llamaService, 
                          ContractDocumentRepository contractRepo,
                          LlmServiceClient llmServiceClient) {
        this.llamaService = llamaService;
        this.contractRepo = contractRepo;
        this.llmServiceClient = llmServiceClient;
    }

    public String analyzeContract(ContractDocument contract) {
        // 保存合同内容到数据库
        contractRepo.save(contract);

        // 构造 LLM prompt
        String prompt = String.format("""
            Read the following contract clause and return a JSON array of risky clauses. Each clause should have a "clause", "reason", and "risk level" field. Respond with JSON only, no explanation.

            Clause:
            %s
            """, contract.getContent());

        // 返回分析结果
        return llamaService.sendPrompt(prompt);
    }
    
    @CircuitBreaker(name = "llm-service", fallbackMethod = "analyzeContractFallback")
    @Retry(name = "llm-service")
    @TimeLimiter(name = "llm-service")
    public CompletableFuture<ContractAnalysisResponse> analyzeContractWithCircuitBreaker(
            ContractAnalysisRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Analyzing contract with correlation ID: {}", correlationId);
        
        return CompletableFuture.supplyAsync(() -> 
            llmServiceClient.analyzeContract(request, correlationId, SERVICE_AUTH_KEY)
        );
    }
    
    public CompletableFuture<ContractAnalysisResponse> analyzeContractFallback(
            ContractAnalysisRequest request, Exception ex) {
        log.error("Circuit breaker fallback triggered for contract analysis: {}", 
            ex.getMessage());
        
        ContractAnalysisResponse fallbackResponse = new ContractAnalysisResponse();
        fallbackResponse.setStatus("FALLBACK");
        fallbackResponse.setSummary("Analysis service temporarily unavailable");
        fallbackResponse.setRiskLevel("UNKNOWN");
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    public List<ContractDocument> getAllContracts() {
    return contractRepo.findAll();
}
}