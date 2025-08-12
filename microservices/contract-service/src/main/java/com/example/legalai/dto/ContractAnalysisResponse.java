package com.example.legalai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAnalysisResponse {
    
    private String analysisId;
    
    private String contractTitle;
    
    private List<Map<String, Object>> analysisResults;
    
    private RiskAssessment riskAssessment;
    
    private ComplianceCheck complianceCheck;
    
    private LocalDateTime analyzedAt;
    
    private long processingTimeMs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessment {
        private String level;
        private double score;
        private List<String> identifiedRisks;
        private List<String> recommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheck {
        private boolean isCompliant;
        private List<String> violations;
        private List<String> suggestions;
    }
}