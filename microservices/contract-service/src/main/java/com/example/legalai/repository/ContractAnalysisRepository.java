package com.example.legalai.repository;

import com.example.legalai.model.ContractAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractAnalysisRepository extends JpaRepository<ContractAnalysis, Long>, JpaSpecificationExecutor<ContractAnalysis> {
    
    Optional<ContractAnalysis> findByAnalysisId(String analysisId);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.contract.id = :contractId ORDER BY ca.createdAt DESC")
    Page<ContractAnalysis> findByContractIdOrderByCreatedAtDesc(@Param("contractId") Long contractId, Pageable pageable);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.contract.id = :contractId AND ca.analysisType = :type ORDER BY ca.createdAt DESC")
    Optional<ContractAnalysis> findLatestByContractIdAndType(@Param("contractId") Long contractId, @Param("type") ContractAnalysis.AnalysisType type);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.status = :status")
    List<ContractAnalysis> findByStatus(@Param("status") ContractAnalysis.AnalysisStatus status);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.riskLevel = :riskLevel")
    Page<ContractAnalysis> findByRiskLevel(@Param("riskLevel") ContractAnalysis.RiskLevel riskLevel, Pageable pageable);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.complianceStatus = false")
    Page<ContractAnalysis> findNonCompliantAnalyses(Pageable pageable);
    
    @Query("SELECT COUNT(ca) FROM ContractAnalysis ca WHERE ca.status = :status")
    long countByStatus(@Param("status") ContractAnalysis.AnalysisStatus status);
    
    @Query("SELECT ca.riskLevel, COUNT(ca) FROM ContractAnalysis ca WHERE ca.status = 'COMPLETED' GROUP BY ca.riskLevel")
    List<Object[]> getRiskLevelStatistics();
    
    @Query("SELECT AVG(ca.processingTimeMs) FROM ContractAnalysis ca WHERE ca.status = 'COMPLETED' AND ca.processingTimeMs IS NOT NULL")
    Optional<Double> getAverageProcessingTime();
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.createdAt >= :since AND ca.createdAt < :until")
    List<ContractAnalysis> findByCreatedAtBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);
    
    @Query("SELECT ca FROM ContractAnalysis ca WHERE ca.contract.owner.id = :ownerId ORDER BY ca.createdAt DESC")
    Page<ContractAnalysis> findByContractOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId, Pageable pageable);
    
    @Query("DELETE FROM ContractAnalysis ca WHERE ca.status IN ('COMPLETED', 'FAILED') AND ca.createdAt < :cutoffDate")
    void deleteOldAnalyses(@Param("cutoffDate") LocalDateTime cutoffDate);
}