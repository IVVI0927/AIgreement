package com.example.legalai.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignClientConfiguration {
    
    @Value("${service.auth.key:${SERVICE_AUTH_KEY:default-service-key}}")
    private String serviceAuthKey;
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Service-Auth", serviceAuthKey);
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Content-Type", "application/json");
        };
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomFeignErrorDecoder();
    }
    
    public static class CustomFeignErrorDecoder implements ErrorDecoder {
        
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign client error - Method: {}, Status: {}", 
                methodKey, response.status());
            
            switch (response.status()) {
                case 400:
                    return new IllegalArgumentException("Bad request to service: " + methodKey);
                case 401:
                    return new SecurityException("Unauthorized access to service: " + methodKey);
                case 404:
                    return new RuntimeException("Service endpoint not found: " + methodKey);
                case 503:
                    return new RuntimeException("Service unavailable: " + methodKey);
                default:
                    return new RuntimeException("Service call failed: " + methodKey + 
                        " with status: " + response.status());
            }
        }
    }
}