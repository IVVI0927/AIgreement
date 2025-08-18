package com.example.legalai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .slowCallRateThreshold(100)
            .slowCallDurationThreshold(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(IOException.class, TimeoutException.class, ConnectException.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        
        registry.getEventPublisher()
            .onEntryAdded(entryAddedEvent -> {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> 
                        log.info("Circuit breaker {} state transition from {} to {}", 
                            event.getCircuitBreakerName(), 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                    .onError(event ->
                        log.error("Circuit breaker {} error: {}", 
                            event.getCircuitBreakerName(), 
                            event.getThrowable().getMessage()))
                    .onCallNotPermitted(event ->
                        log.warn("Circuit breaker {} call not permitted", 
                            event.getCircuitBreakerName()));
            });
        
        return registry;
    }
    
    @Bean
    RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1000))
            .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
            .retryOnException(throwable -> 
                throwable instanceof ConnectException || 
                throwable instanceof IOException ||
                throwable instanceof TimeoutException)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
        
        RetryRegistry registry = RetryRegistry.of(retryConfig);
        
        registry.getEventPublisher()
            .onEntryAdded(entryAddedEvent -> {
                Retry retry = entryAddedEvent.getAddedEntry();
                retry.getEventPublisher()
                    .onRetry(event -> 
                        log.info("Retry {} attempt #{}", 
                            event.getName(), 
                            event.getNumberOfRetryAttempts()))
                    .onError(event ->
                        log.error("Retry {} failed after {} attempts: {}", 
                            event.getName(), 
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()));
            });
        
        return registry;
    }
    
    @Bean
    TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(30))
            .cancelRunningFuture(true)
            .build();
        
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(timeLimiterConfig);
        
        registry.getEventPublisher()
            .onEntryAdded(entryAddedEvent -> {
                TimeLimiter timeLimiter = entryAddedEvent.getAddedEntry();
                timeLimiter.getEventPublisher()
                    .onTimeout(event -> 
                        log.warn("TimeLimiter {} timeout after {}ms", 
                            event.getTimeLimiterName(), 
                            event.getTimeoutDuration().toMillis()));
            });
        
        return registry;
    }
    
    @Bean
    CircuitBreaker llmServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("llm-service");
    }
    
    @Bean
    Retry llmServiceRetry(RetryRegistry registry) {
        return registry.retry("llm-service");
    }
    
    @Bean
    TimeLimiter llmServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("llm-service");
    }
    
    public static class IntervalFunction {
        public static io.github.resilience4j.core.IntervalFunction ofExponentialBackoff(long initialInterval, double multiplier) {
            return io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(initialInterval, multiplier);
        }
    }
}