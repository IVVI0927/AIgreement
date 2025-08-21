package com.example.legalai.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.endpoint.annotation.Endpoint;
import org.springframework.boot.actuator.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/actuator/performance")
public class PerformanceMonitoringController {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    @Autowired
    public PerformanceMonitoringController(MeterRegistry meterRegistry, DataSource dataSource) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        initializeMetrics();
    }

    private void initializeMetrics() {
        // Register custom gauges
        Gauge.builder("database.connections.active")
                .register(meterRegistry, activeConnections, AtomicLong::get);
        
        Gauge.builder("api.requests.total")
                .register(meterRegistry, totalRequests, AtomicLong::get);
        
        Gauge.builder("api.errors.total")
                .register(meterRegistry, errorCount, AtomicLong::get);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Database performance metrics
            long start = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                long connectionTime = System.currentTimeMillis() - start;
                metrics.put("database.connection.time.ms", connectionTime);
                metrics.put("database.connection.status", "healthy");
            }
        } catch (SQLException e) {
            metrics.put("database.connection.status", "unhealthy");
            metrics.put("database.connection.error", e.getMessage());
        }

        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        metrics.put("jvm.memory.used.bytes", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("jvm.memory.free.bytes", runtime.freeMemory());
        metrics.put("jvm.memory.total.bytes", runtime.totalMemory());
        metrics.put("jvm.memory.max.bytes", runtime.maxMemory());
        metrics.put("jvm.memory.usage.percent", 
                (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100);

        // Application metrics
        metrics.put("api.requests.total", totalRequests.get());
        metrics.put("api.errors.total", errorCount.get());
        metrics.put("api.error.rate.percent", 
                totalRequests.get() > 0 ? (double) errorCount.get() / totalRequests.get() * 100 : 0);

        // System metrics
        metrics.put("system.cpu.processors", runtime.availableProcessors());
        metrics.put("timestamp", LocalDateTime.now());

        return metrics;
    }

    @GetMapping("/latency")
    public Map<String, Object> getLatencyMetrics() {
        Map<String, Object> latencyMetrics = new HashMap<>();
        
        // Get timer metrics from registry
        Timer requestTimer = Timer.builder("http.server.requests")
                .register(meterRegistry);
        
        latencyMetrics.put("response.time.mean.ms", requestTimer.mean(TimeUnit.MILLISECONDS));
        latencyMetrics.put("response.time.max.ms", requestTimer.max(TimeUnit.MILLISECONDS));
        latencyMetrics.put("response.time.p95.ms", 
                requestTimer.percentile(0.95, TimeUnit.MILLISECONDS));
        latencyMetrics.put("response.time.p99.ms", 
                requestTimer.percentile(0.99, TimeUnit.MILLISECONDS));
        
        return latencyMetrics;
    }

    @GetMapping("/throughput")
    public Map<String, Object> getThroughputMetrics() {
        Map<String, Object> throughputMetrics = new HashMap<>();
        
        Counter requestCounter = Counter.builder("http.server.requests.total")
                .register(meterRegistry);
        
        throughputMetrics.put("requests.per.second", requestCounter.count() / 60); // Approximate RPS
        throughputMetrics.put("total.requests", requestCounter.count());
        throughputMetrics.put("timestamp", LocalDateTime.now());
        
        return throughputMetrics;
    }

    @GetMapping("/database")
    public Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> dbMetrics = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Connection pool metrics
            javax.sql.DataSource hikariDataSource = dataSource;
            if (hikariDataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikari = 
                        (com.zaxxer.hikari.HikariDataSource) hikariDataSource;
                
                com.zaxxer.hikari.HikariPoolMXBean poolBean = hikari.getHikariPoolMXBean();
                if (poolBean != null) {
                    dbMetrics.put("pool.active.connections", poolBean.getActiveConnections());
                    dbMetrics.put("pool.idle.connections", poolBean.getIdleConnections());
                    dbMetrics.put("pool.total.connections", poolBean.getTotalConnections());
                    dbMetrics.put("pool.threads.waiting", poolBean.getThreadsAwaitingConnection());
                }
            }
            
            dbMetrics.put("connection.status", "healthy");
        } catch (SQLException e) {
            dbMetrics.put("connection.status", "unhealthy");
            dbMetrics.put("error", e.getMessage());
        }
        
        return dbMetrics;
    }

    // Utility methods for tracking
    public void incrementRequestCount() {
        totalRequests.incrementAndGet();
    }

    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }

    public void recordConnectionUsage(boolean active) {
        if (active) {
            activeConnections.incrementAndGet();
        } else {
            activeConnections.decrementAndGet();
        }
    }

    @Component("performanceHealthIndicator")
    public static class PerformanceHealthIndicator implements HealthIndicator {

        private final DataSource dataSource;
        private final Runtime runtime = Runtime.getRuntime();

        public PerformanceHealthIndicator(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Health health() {
            Health.Builder healthBuilder = new Health.Builder();

            // Check memory usage
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
            if (memoryUsage > 0.9) {
                healthBuilder.down().withDetail("memory.usage", "Critical: " + (memoryUsage * 100) + "%");
            } else if (memoryUsage > 0.8) {
                healthBuilder.status("WARNING").withDetail("memory.usage", "High: " + (memoryUsage * 100) + "%");
            } else {
                healthBuilder.up().withDetail("memory.usage", "Normal: " + (memoryUsage * 100) + "%");
            }

            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                long start = System.currentTimeMillis();
                connection.isValid(5);
                long responseTime = System.currentTimeMillis() - start;
                
                if (responseTime > 1000) {
                    healthBuilder.down().withDetail("database.response.time", responseTime + "ms (too slow)");
                } else {
                    healthBuilder.withDetail("database.response.time", responseTime + "ms");
                }
            } catch (SQLException e) {
                healthBuilder.down().withDetail("database.error", e.getMessage());
            }

            return healthBuilder.build();
        }
    }
}

@Component
@Endpoint(id = "performance-details")
class PerformanceDetailsEndpoint {
    
    private final PerformanceMonitoringController performanceController;
    
    public PerformanceDetailsEndpoint(PerformanceMonitoringController performanceController) {
        this.performanceController = performanceController;
    }
    
    @ReadOperation
    public Map<String, Object> performanceDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("metrics", performanceController.getPerformanceMetrics());
        details.put("latency", performanceController.getLatencyMetrics());
        details.put("throughput", performanceController.getThroughputMetrics());
        details.put("database", performanceController.getDatabaseMetrics());
        return details;
    }
}