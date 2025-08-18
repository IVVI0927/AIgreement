package com.example.legalai.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Health.Builder healthBuilder = Health.up();
        boolean hasOpenCircuit = false;
        
        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", state.toString());
            cbDetails.put("failureRate", metrics.getFailureRate());
            cbDetails.put("slowCallRate", metrics.getSlowCallRate());
            cbDetails.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
            cbDetails.put("failedCalls", metrics.getNumberOfFailedCalls());
            cbDetails.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
            
            details.put(name, cbDetails);
            
            if (state == CircuitBreaker.State.OPEN) {
                hasOpenCircuit = true;
                log.warn("Circuit breaker {} is OPEN", name);
            }
        }
        
        if (hasOpenCircuit) {
            healthBuilder = Health.down()
                .withDetail("reason", "One or more circuit breakers are open");
        }
        
        return healthBuilder
            .withDetail("circuitBreakers", details)
            .build();
    }
}