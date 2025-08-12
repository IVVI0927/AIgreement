package com.example.legalai.repository;

import com.example.legalai.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType AND al.entityId = :entityId ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            @Param("entityType") String entityType, 
            @Param("entityId") Long entityId, 
            Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.user.id = :userId ORDER BY al.createdAt DESC")
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionAndCreatedAtAfter(@Param("action") AuditLog.AuditAction action, @Param("since") LocalDateTime since);
    
    @Query("SELECT al FROM AuditLog al WHERE al.correlationId = :correlationId ORDER BY al.createdAt ASC")
    List<AuditLog> findByCorrelationIdOrderByCreatedAtAsc(@Param("correlationId") String correlationId);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action AND al.createdAt >= :since")
    long countByActionAndCreatedAtAfter(@Param("action") AuditLog.AuditAction action, @Param("since") LocalDateTime since);
    
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE al.createdAt >= :since GROUP BY al.action")
    List<Object[]> getActionStatisticsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByIpAddressAndCreatedAtAfter(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT DISTINCT al.ipAddress FROM AuditLog al WHERE al.user.id = :userId AND al.createdAt >= :since")
    List<String> findDistinctIpAddressesByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}