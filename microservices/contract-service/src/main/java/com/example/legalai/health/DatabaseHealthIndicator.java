package com.example.legalai.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                Long contractCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM contracts", Long.class);
                
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .withDetail("contractCount", contractCount)
                    .build();
            }
            
            return Health.down()
                .withDetail("error", "Database check query failed")
                .build();
                
        } catch (Exception ex) {
            log.error("Database health check failed", ex);
            return Health.down()
                .withDetail("error", ex.getMessage())
                .withException(ex)
                .build();
        }
    }
}