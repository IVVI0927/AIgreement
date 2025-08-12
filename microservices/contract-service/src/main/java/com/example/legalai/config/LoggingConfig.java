package com.example.legalai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Configuration
public class LoggingConfig {

    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Slf4j
    public static class RequestResponseLoggingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String correlationId = request.getHeader("X-Correlation-Id");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            
            MDC.put("correlationId", correlationId);
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestUri", request.getRequestURI());
            
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            
            long startTime = System.currentTimeMillis();
            
            try {
                filterChain.doFilter(requestWrapper, responseWrapper);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                String requestBody = getStringValue(requestWrapper.getContentAsByteArray(), 
                        request.getCharacterEncoding());
                String responseBody = getStringValue(responseWrapper.getContentAsByteArray(), 
                        response.getCharacterEncoding());
                
                if (!request.getRequestURI().contains("/actuator")) {
                    log.info("Request: method={}, uri={}, correlationId={}, body={}", 
                            request.getMethod(), 
                            request.getRequestURI(), 
                            correlationId,
                            maskSensitiveData(requestBody));
                    
                    log.info("Response: status={}, correlationId={}, duration={}ms, body={}", 
                            response.getStatus(), 
                            correlationId,
                            duration,
                            maskSensitiveData(responseBody));
                }
                
                responseWrapper.copyBodyToResponse();
                response.setHeader("X-Correlation-Id", correlationId);
                
                MDC.clear();
            }
        }
        
        private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
            try {
                return new String(contentAsByteArray, characterEncoding);
            } catch (UnsupportedEncodingException e) {
                log.error("Error converting byte array to string", e);
                return "";
            }
        }
        
        private String maskSensitiveData(String data) {
            if (data == null || data.isEmpty()) {
                return data;
            }
            
            return data.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                      .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                      .replaceAll("\"apiKey\"\\s*:\\s*\"[^\"]*\"", "\"apiKey\":\"***\"");
        }
    }
}