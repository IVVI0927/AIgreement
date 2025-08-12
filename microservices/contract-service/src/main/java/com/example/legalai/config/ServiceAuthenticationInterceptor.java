package com.example.legalai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ServiceAuthenticationInterceptor implements HandlerInterceptor {

    @Value("${service.api.key:${SERVICE_API_KEY:defaultServiceApiKeyForInternalCommunication}}")
    private String expectedApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        if (path.startsWith("/actuator/health") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        String serviceAuth = request.getHeader("X-Service-Auth");
        
        if (serviceAuth == null || !serviceAuth.equals(expectedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized service request\"}");
            return false;
        }
        
        return true;
    }
}