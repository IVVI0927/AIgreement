package com.example.legalAI.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import com.example.legalAI.util.FileTextExtractor;
import org.springframework.http.HttpStatus;
import com.example.legalAI.model.ContractDocument;
import com.example.legalAI.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeContract(@RequestBody ContractDocument contract) {
        String result = contractService.analyzeContract(contract);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> parsedResult;
        try {
            parsedResult = mapper.readValue(result, new TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to parse analysis result"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("title", contract.getTitle());
        response.put("analysisResult", parsedResult);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public List<ContractDocument> getAllContracts() {
        return contractService.getAllContracts();
    }

    /**
     * 接收 PDF 或 DOCX 文件并提取其文本内容
     * 用于合同内容自动填充
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadContract(@RequestParam("file") MultipartFile file) {
        try {
            String content = FileTextExtractor.extractText(file);
            Map<String, String> response = new HashMap<>();
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to extract content: " + e.getMessage()));
        }
    }

}
