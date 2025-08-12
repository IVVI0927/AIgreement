package com.example.legalai.repository;

import com.example.legalai.model.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, Long> {
    // 可选添加自定义查询方法
}
