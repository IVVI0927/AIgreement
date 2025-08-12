package com.example.legalai.exception;

import com.example.legalai.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        Map<String, List<String>> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });
        
        log.warn("Validation failed. CorrelationId: {}, Errors: {}", correlationId, errors);
        
        ErrorResponse errorResponse = ErrorResponse.validationError(errors, correlationId);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        Map<String, List<String>> errors = new HashMap<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.computeIfAbsent(propertyPath, k -> new ArrayList<>()).add(message);
        }
        
        log.warn("Constraint violation. CorrelationId: {}, Errors: {}", correlationId, errors);
        
        ErrorResponse errorResponse = ErrorResponse.validationError(errors, correlationId);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        log.error("Business exception occurred. CorrelationId: {}, Message: {}", 
                correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                correlationId
        );
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        errorResponse.setDetails(ex.getDetails());
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        log.warn("Resource not found. CorrelationId: {}, Message: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                correlationId
        );
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        log.warn("File upload size exceeded. CorrelationId: {}", correlationId);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "FILE_TOO_LARGE",
                "File size exceeds maximum allowed size",
                correlationId
        );
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        log.error("Data integrity violation. CorrelationId: {}", correlationId, ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "DATA_INTEGRITY_ERROR",
                "Data integrity constraint violated",
                correlationId
        );
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        String correlationId = getCorrelationId();
        log.error("Unexpected error occurred. CorrelationId: {}", correlationId, ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                correlationId
        );
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}