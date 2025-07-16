package com.example.legalAI.service;

import com.example.legalAI.model.ContractDocument;
import com.example.legalAI.llm.LlamaService;
import com.example.legalAI.repository.ContractDocumentRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContractService {

    private final LlamaService llamaService;
    private final ContractDocumentRepository contractRepo;

    @Autowired
    public ContractService(LlamaService llamaService, ContractDocumentRepository contractRepo) {
        this.llamaService = llamaService;
        this.contractRepo = contractRepo;
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
    public List<ContractDocument> getAllContracts() {
    return contractRepo.findAll();
}
}