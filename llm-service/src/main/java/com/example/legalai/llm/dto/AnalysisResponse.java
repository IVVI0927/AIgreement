package com.example.legalai.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private List<Map<String, Object>> analysisResult;
    private String modelUsed;
    private String error;
    private String taskId;
    private String status; // "completed", "processing", "failed"
    private Long processingTimeMs;
} 