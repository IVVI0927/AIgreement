package com.example.legalai.repository;

import com.example.legalai.model.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizedContractRepository extends JpaRepository<ContractDocument, String> {

    // Optimized query with specific indexes
    @Query(value = """
        SELECT c.* FROM contracts c 
        WHERE c.owner_id = :ownerId 
        AND c.status = :status 
        AND c.is_deleted = false
        ORDER BY c.updated_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ContractDocument> findByOwnerAndStatusOptimized(
        @Param("ownerId") String ownerId, 
        @Param("status") String status,
        @Param("limit") int limit
    );

    // Prepared statement for batch operations
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE contracts 
        SET risk_score = :riskScore, 
            analysis_status = :analysisStatus,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :contractId
        """, nativeQuery = true)
    int updateContractAnalysisOptimized(
        @Param("contractId") String contractId,
        @Param("riskScore") Integer riskScore,
        @Param("analysisStatus") String analysisStatus
    );

    // Optimized search with full-text indexing
    @Query(value = """
        SELECT c.*, ts_rank(to_tsvector('english', c.content), plainto_tsquery('english', :searchTerm)) as rank
        FROM contracts c
        WHERE to_tsvector('english', c.content) @@ plainto_tsquery('english', :searchTerm)
        AND c.is_deleted = false
        ORDER BY rank DESC, c.updated_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ContractDocument> searchContractsFullText(
        @Param("searchTerm") String searchTerm,
        @Param("limit") int limit
    );

    // Optimized aggregation query
    @Query(value = """
        SELECT 
            COUNT(*) as total_contracts,
            COUNT(CASE WHEN risk_score > 70 THEN 1 END) as high_risk_count,
            COUNT(CASE WHEN analysis_status = 'PENDING' THEN 1 END) as pending_count,
            AVG(risk_score) as avg_risk_score
        FROM contracts 
        WHERE owner_id = :ownerId 
        AND is_deleted = false
        """, nativeQuery = true)
    Object[] getContractStatisticsOptimized(@Param("ownerId") String ownerId);

    // Batch insert optimization
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO contracts (id, title, content, owner_id, status, created_at, updated_at, is_deleted)
        VALUES (:id, :title, :content, :ownerId, :status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
        ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        content = EXCLUDED.content,
        updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertContractOptimized(
        @Param("id") String id,
        @Param("title") String title,
        @Param("content") String content,
        @Param("ownerId") String ownerId,
        @Param("status") String status
    );

    // Index hint for performance critical queries
    @Query(value = """
        SELECT /*+ INDEX(contracts, idx_contracts_owner_status) */ c.*
        FROM contracts c
        WHERE c.owner_id = :ownerId
        AND c.created_at BETWEEN :startDate AND :endDate
        AND c.is_deleted = false
        ORDER BY c.created_at DESC
        """, nativeQuery = true)
    List<ContractDocument> findContractsByDateRangeOptimized(
        @Param("ownerId") String ownerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // Connection pool optimized query
    @Query(value = """
        SELECT c.id, c.title, c.risk_score, c.status
        FROM contracts c
        WHERE c.updated_at > :since
        AND c.is_deleted = false
        ORDER BY c.updated_at DESC
        """, nativeQuery = true)
    List<Object[]> getRecentContractsLightweight(@Param("since") LocalDateTime since);
}