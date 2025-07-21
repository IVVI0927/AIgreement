package com.example.legalai.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    private String content;
    private String title;
    private String userId;
    private String analysisType; // "risk", "compliance", "summary"
    private String language; // "en", "zh"
} 