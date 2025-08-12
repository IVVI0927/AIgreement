package com.example.legalai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    
    private String message;
    
    private String details;
    
    private String correlationId;
    
    private String path;
    
    private LocalDateTime timestamp;
    
    private Map<String, List<String>> validationErrors;
    
    public static ErrorResponse of(String errorCode, String message, String correlationId) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse validationError(Map<String, List<String>> errors, String correlationId) {
        return ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed")
                .validationErrors(errors)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}