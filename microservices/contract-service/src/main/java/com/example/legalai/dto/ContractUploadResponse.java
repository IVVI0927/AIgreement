package com.example.legalai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractUploadResponse {
    
    private String fileName;
    
    private String fileType;
    
    private long fileSize;
    
    private String extractedContent;
    
    private int wordCount;
    
    private String uploadId;
    
    private String message;
}