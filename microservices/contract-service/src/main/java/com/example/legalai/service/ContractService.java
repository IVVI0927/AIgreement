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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @CacheEvict(value = "contracts", allEntries = true)
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

        ContractAnalysisResponse fallbackResponse = ContractAnalysisResponse.builder()
            .contractTitle(request.getTitle())
            .analyzedAt(LocalDateTime.now())
            .analysisResults(List.of(Map.of(
                "status", "FALLBACK",
                "summary", "Analysis service temporarily unavailable",
                "riskLevel", "UNKNOWN"
            )))
            .build();

        return CompletableFuture.completedFuture(fallbackResponse);
    }
    @Cacheable(value = "contracts", key = "'all-contracts'")
    public List<ContractDocument> getAllContracts() {
        log.info("Fetching all contracts from database");
        return contractRepo.findAll();
    }

    public Optional<ContractDocument> getContractById(String id) {
        try {
            return contractRepo.findById(Long.parseLong(id));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @CacheEvict(value = "contracts", allEntries = true)
    public ContractDocument updateContract(String id, ContractDocument contract) {
        Long contractId = parseIdOrThrow(id);
        ContractDocument existing = contractRepo.findById(contractId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + id));
        existing.setTitle(contract.getTitle());
        existing.setContent(contract.getContent());
        return contractRepo.save(existing);
    }

    @CacheEvict(value = "contracts", allEntries = true)
    public void deleteContract(String id) {
        Long contractId = parseIdOrThrow(id);
        contractRepo.deleteById(contractId);
    }

    public Map<String, Object> getContractStatistics() {
        long total = contractRepo.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalContracts", total);
        stats.put("timestamp", LocalDateTime.now().toString());
        return stats;
    }

    public Map<String, Object> addReview(String id, Map<String, Object> review) {
        Map<String, Object> result = new HashMap<>();
        result.put("contractId", id);
        result.put("review", review);
        result.put("status", "RECORDED");
        result.put("reviewedAt", LocalDateTime.now().toString());
        return result;
    }

    public List<Map<String, Object>> getContractHistory(String id) {
        Optional<ContractDocument> contractOpt = getContractById(id);
        if (contractOpt.isEmpty()) {
            return List.of();
        }
        ContractDocument contract = contractOpt.get();
        return List.of(Map.of(
            "contractId", contract.getId(),
            "title", contract.getTitle(),
            "event", "CURRENT_VERSION",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    public List<Map<String, Object>> batchAnalyze(List<ContractDocument> contracts) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (ContractDocument contract : contracts) {
            String analysis = analyzeContract(contract);
            results.add(Map.of(
                "title", contract.getTitle(),
                "analysisResult", analysis
            ));
        }
        return results;
    }

    public List<ContractDocument> searchContracts(String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return contractRepo.findAll().stream()
            .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                || (c.getContent() != null && c.getContent().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
            .toList();
    }

    public byte[] exportContract(String id, String format) {
        ContractDocument contract = getContractById(id)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + id));
        String payload = "Title: " + contract.getTitle() + "\n\n" + contract.getContent();
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    private Long parseIdOrThrow(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid contract id: " + id, ex);
        }
    }
}
