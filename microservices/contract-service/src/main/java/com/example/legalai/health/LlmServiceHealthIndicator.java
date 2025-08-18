package com.example.legalai.health;

import com.example.legalai.client.LlmServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmServiceHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public Health health() {
        try {
            String llmServiceUrl = "http://llm-service:8083/actuator/health";
            String response = restTemplate.getForObject(llmServiceUrl, String.class);
            
            if (response != null && response.contains("UP")) {
                return Health.up()
                    .withDetail("service", "llm-service")
                    .withDetail("status", "Connected")
                    .withDetail("url", llmServiceUrl)
                    .build();
            }
            
            return Health.down()
                .withDetail("service", "llm-service")
                .withDetail("status", "Not responding correctly")
                .build();
                
        } catch (RestClientException ex) {
            log.warn("LLM service health check failed: {}", ex.getMessage());
            return Health.down()
                .withDetail("service", "llm-service")
                .withDetail("error", ex.getMessage())
                .build();
        }
    }
}