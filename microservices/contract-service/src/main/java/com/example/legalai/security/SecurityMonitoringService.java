package com.example.legalai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class SecurityMonitoringService {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, AtomicInteger> rateLimitMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failedAttemptsMap = new ConcurrentHashMap<>();
    private final AtomicInteger totalSecurityEvents = new AtomicInteger(0);
    private final AtomicInteger blockedRequests = new AtomicInteger(0);

    @Autowired
    public SecurityMonitoringService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void logSecurityEvent(SecurityEventType eventType, String details, String userId) {
        HttpServletRequest request = getCurrentRequest();
        String clientIp = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
        
        SecurityEvent event = SecurityEvent.builder()
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .clientIp(clientIp)
                .userId(userId)
                .userAgent(userAgent)
                .details(details)
                .build();

        // Log the security event
        log.warn("SECURITY_EVENT: {} - IP: {} - User: {} - Details: {}", 
                eventType, clientIp, userId, details);

        // Increment counters
        totalSecurityEvents.incrementAndGet();
        
        // Publish event for further processing
        eventPublisher.publishEvent(event);

        // Check for potential attacks
        checkForSuspiciousActivity(clientIp, eventType);
    }

    public void logSuccessfulAuthentication(String userId) {
        logSecurityEvent(SecurityEventType.SUCCESSFUL_AUTHENTICATION, 
                "User successfully authenticated", userId);
        
        // Reset failed attempts on successful auth
        HttpServletRequest request = getCurrentRequest();
        String clientIp = getClientIpAddress(request);
        failedAttemptsMap.remove(clientIp);
    }

    public void logFailedAuthentication(String userId) {
        HttpServletRequest request = getCurrentRequest();
        String clientIp = getClientIpAddress(request);
        
        // Increment failed attempts
        AtomicInteger attempts = failedAttemptsMap.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int failedCount = attempts.incrementAndGet();
        
        logSecurityEvent(SecurityEventType.FAILED_AUTHENTICATION, 
                "Failed authentication attempt #" + failedCount, userId);
        
        // Block after 5 failed attempts
        if (failedCount >= 5) {
            logSecurityEvent(SecurityEventType.BRUTE_FORCE_ATTACK, 
                    "Multiple failed authentication attempts detected", userId);
            blockedRequests.incrementAndGet();
        }
    }

    public void logUnauthorizedAccess(String resource, String userId) {
        logSecurityEvent(SecurityEventType.UNAUTHORIZED_ACCESS, 
                "Attempted access to resource: " + resource, userId);
    }

    public void logSuspiciousActivity(String activity, String userId) {
        logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, activity, userId);
    }

    public void logCSRFAttempt(String userId) {
        logSecurityEvent(SecurityEventType.CSRF_ATTEMPT, 
                "CSRF token validation failed", userId);
        blockedRequests.incrementAndGet();
    }

    public void logXSSAttempt(String payload, String userId) {
        logSecurityEvent(SecurityEventType.XSS_ATTEMPT, 
                "XSS payload detected: " + sanitizeForLog(payload), userId);
        blockedRequests.incrementAndGet();
    }

    public void logSQLInjectionAttempt(String query, String userId) {
        logSecurityEvent(SecurityEventType.SQL_INJECTION_ATTEMPT, 
                "SQL injection detected in query: " + sanitizeForLog(query), userId);
        blockedRequests.incrementAndGet();
    }

    public boolean isRateLimited(String clientIp, int maxRequests, int timeWindowMinutes) {
        String key = clientIp + "_" + (System.currentTimeMillis() / (timeWindowMinutes * 60 * 1000));
        AtomicInteger requests = rateLimitMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        
        if (requests.incrementAndGet() > maxRequests) {
            logSecurityEvent(SecurityEventType.RATE_LIMIT_EXCEEDED, 
                    "Rate limit exceeded: " + requests.get() + " requests", null);
            return true;
        }
        return false;
    }

    public Map<String, Object> getSecurityMetrics() {
        return Map.of(
                "totalSecurityEvents", totalSecurityEvents.get(),
                "blockedRequests", blockedRequests.get(),
                "activeFailedAttempts", failedAttemptsMap.size(),
                "rateLimitedIps", rateLimitMap.size(),
                "timestamp", LocalDateTime.now()
        );
    }

    private void checkForSuspiciousActivity(String clientIp, SecurityEventType eventType) {
        // Check if this IP has multiple security events in short time
        long recentEvents = totalSecurityEvents.get();
        
        if (recentEvents > 10 && isSuspiciousEventType(eventType)) {
            logSecurityEvent(SecurityEventType.POTENTIAL_ATTACK, 
                    "Multiple security events detected from IP: " + clientIp, null);
        }
    }

    private boolean isSuspiciousEventType(SecurityEventType eventType) {
        return eventType == SecurityEventType.XSS_ATTEMPT ||
               eventType == SecurityEventType.SQL_INJECTION_ATTEMPT ||
               eventType == SecurityEventType.CSRF_ATTEMPT ||
               eventType == SecurityEventType.UNAUTHORIZED_ACCESS;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "Unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        
        // Limit length and remove dangerous characters
        String sanitized = input.length() > 200 ? input.substring(0, 200) + "..." : input;
        return sanitized.replaceAll("[\r\n\t]", "_");
    }

    public enum SecurityEventType {
        SUCCESSFUL_AUTHENTICATION,
        FAILED_AUTHENTICATION,
        UNAUTHORIZED_ACCESS,
        CSRF_ATTEMPT,
        XSS_ATTEMPT,
        SQL_INJECTION_ATTEMPT,
        RATE_LIMIT_EXCEEDED,
        SUSPICIOUS_ACTIVITY,
        BRUTE_FORCE_ATTACK,
        POTENTIAL_ATTACK
    }

    public static class SecurityEvent {
        private SecurityEventType eventType;
        private LocalDateTime timestamp;
        private String clientIp;
        private String userId;
        private String userAgent;
        private String details;

        public static SecurityEventBuilder builder() {
            return new SecurityEventBuilder();
        }

        public static class SecurityEventBuilder {
            private SecurityEventType eventType;
            private LocalDateTime timestamp;
            private String clientIp;
            private String userId;
            private String userAgent;
            private String details;

            public SecurityEventBuilder eventType(SecurityEventType eventType) {
                this.eventType = eventType;
                return this;
            }

            public SecurityEventBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public SecurityEventBuilder clientIp(String clientIp) {
                this.clientIp = clientIp;
                return this;
            }

            public SecurityEventBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public SecurityEventBuilder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }

            public SecurityEventBuilder details(String details) {
                this.details = details;
                return this;
            }

            public SecurityEvent build() {
                SecurityEvent event = new SecurityEvent();
                event.eventType = this.eventType;
                event.timestamp = this.timestamp;
                event.clientIp = this.clientIp;
                event.userId = this.userId;
                event.userAgent = this.userAgent;
                event.details = this.details;
                return event;
            }
        }
    }
}