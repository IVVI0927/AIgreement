package com.example.legalai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractAnalysisRequest {
    
    @NotBlank(message = "Contract title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    @NotBlank(message = "Contract content is required")
    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;
    
    private String contractType;
    
    private String analysisDepth = "standard";
    
    private boolean includeRiskAssessment = true;
    
    private boolean includeCompliance = true;
}