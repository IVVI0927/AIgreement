package com.example.legalai.repository;

import com.example.legalai.model.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    
    @Query("SELECT c FROM Contract c WHERE c.deleted = false")
    Page<Contract> findAllActive(Pageable pageable);
    
    @Query("SELECT c FROM Contract c WHERE c.owner.id = :ownerId AND c.deleted = false")
    Page<Contract> findByOwnerIdAndActiveTrue(@Param("ownerId") Long ownerId, Pageable pageable);
    
    @Query("SELECT c FROM Contract c WHERE c.id = :id AND c.deleted = false")
    Optional<Contract> findByIdAndActiveTrue(@Param("id") Long id);
    
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.deleted = false")
    Page<Contract> findByStatusAndActiveTrue(@Param("status") Contract.ContractStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Contract c WHERE c.contractType = :type AND c.deleted = false")
    Page<Contract> findByContractTypeAndActiveTrue(@Param("type") Contract.ContractType type, Pageable pageable);
    
    @Query("SELECT c FROM Contract c WHERE c.deleted = false AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contract> findBySearchTermAndActiveTrue(@Param("search") String searchTerm, Pageable pageable);
    
    @Query("SELECT c FROM Contract c WHERE c.parentContract.id = :parentId AND c.deleted = false ORDER BY c.version DESC")
    List<Contract> findVersionsByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT c FROM Contract c WHERE c.parentContract.id = :parentId AND c.deleted = false ORDER BY c.version DESC")
    Optional<Contract> findLatestVersionByParentId(@Param("parentId") Long parentId);
    
    @Modifying
    @Query("UPDATE Contract c SET c.deleted = true, c.deletedAt = :deletedAt WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
    
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.owner.id = :ownerId AND c.deleted = false")
    long countByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status AND c.deleted = false")
    long countByStatus(@Param("status") Contract.ContractStatus status);
    
    @Query("SELECT c.contractType, COUNT(c) FROM Contract c WHERE c.deleted = false GROUP BY c.contractType")
    List<Object[]> getContractTypeStatistics();
    
    @Query("SELECT c FROM Contract c WHERE c.createdAt >= :since AND c.deleted = false")
    List<Contract> findCreatedSince(@Param("since") LocalDateTime since);
}