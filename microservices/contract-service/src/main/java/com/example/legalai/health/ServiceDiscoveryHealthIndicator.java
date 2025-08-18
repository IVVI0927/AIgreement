package com.example.legalai.health;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceDiscoveryHealthIndicator implements HealthIndicator {
    
    private final DiscoveryClient discoveryClient;
    private final EurekaClient eurekaClient;
    
    @Override
    public Health health() {
        try {
            List<String> services = discoveryClient.getServices();
            Map<String, Object> details = new HashMap<>();
            
            details.put("registeredServices", services);
            details.put("serviceCount", services.size());
            
            for (String service : services) {
                Application app = eurekaClient.getApplication(service.toUpperCase());
                if (app != null) {
                    details.put(service + "_instances", app.getInstances().size());
                }
            }
            
            boolean eurekaConnected = !eurekaClient.getApplications().getRegisteredApplications().isEmpty();
            
            if (eurekaConnected) {
                return Health.up()
                    .withDetail("eurekaStatus", "Connected")
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetail("eurekaStatus", "No services discovered")
                    .withDetails(details)
                    .build();
            }
            
        } catch (Exception ex) {
            log.error("Service discovery health check failed", ex);
            return Health.down()
                .withDetail("error", ex.getMessage())
                .withException(ex)
                .build();
        }
    }
}