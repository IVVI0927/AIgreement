package com.example.legalai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contracts", indexes = {
    @Index(name = "idx_contract_owner", columnList = "owner_id"),
    @Index(name = "idx_contract_status", columnList = "status"),
    @Index(name = "idx_contract_type", columnList = "contract_type"),
    @Index(name = "idx_contract_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Contract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Lob
    @Column(nullable = false)
    private String content;
    
    @Column(name = "contract_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ContractType contractType;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_hash")
    private String fileHash;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.DRAFT;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_contract_id")
    private Contract parentContract;
    
    @OneToMany(mappedBy = "parentContract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Contract> versions = new HashSet<>();
    
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ContractAnalysis> analyses = new HashSet<>();
    
    @Column(name = "is_deleted")
    private boolean deleted = false;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    public enum ContractType {
        SERVICE_AGREEMENT,
        NDA,
        EMPLOYMENT_CONTRACT,
        LEASE_AGREEMENT,
        PURCHASE_AGREEMENT,
        LICENSING_AGREEMENT,
        PARTNERSHIP_AGREEMENT,
        OTHER
    }
    
    public enum ContractStatus {
        DRAFT,
        UNDER_REVIEW,
        APPROVED,
        SIGNED,
        EXECUTED,
        EXPIRED,
        TERMINATED
    }
}