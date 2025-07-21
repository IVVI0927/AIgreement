package com.example.legalai.llm.controller;

import com.example.legalai.llm.service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@CrossOrigin(origins = "*")
public class LlmController {

    @Autowired
    private LlmService llmService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeContract(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String result = llmService.analyzeContract(content);
            
            Map<String, Object> response = Map.of(
                "analysisResult", result,
                "status", "success"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
} 