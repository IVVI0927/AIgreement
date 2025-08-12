package com.example.legalai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_analyses", indexes = {
    @Index(name = "idx_analysis_contract", columnList = "contract_id"),
    @Index(name = "idx_analysis_status", columnList = "status"),
    @Index(name = "idx_analysis_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContractAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "analysis_id", unique = true, nullable = false)
    private String analysisId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
    
    @Column(name = "analysis_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisType analysisType;
    
    @Lob
    @Column(name = "analysis_result")
    private String analysisResult;
    
    @Column(name = "risk_score")
    private Double riskScore;
    
    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    @Lob
    @Column(name = "identified_risks")
    private String identifiedRisks;
    
    @Lob
    @Column(name = "recommendations")
    private String recommendations;
    
    @Column(name = "compliance_status")
    private Boolean complianceStatus;
    
    @Lob
    @Column(name = "compliance_violations")
    private String complianceViolations;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status = AnalysisStatus.PENDING;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public enum AnalysisType {
        FULL_ANALYSIS,
        RISK_ASSESSMENT,
        COMPLIANCE_CHECK,
        CLAUSE_EXTRACTION,
        SUMMARY_GENERATION
    }
    
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum AnalysisStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}