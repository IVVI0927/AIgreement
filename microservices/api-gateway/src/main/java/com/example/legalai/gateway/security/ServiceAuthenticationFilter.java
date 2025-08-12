package com.example.legalai.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class ServiceAuthenticationFilter extends AbstractGatewayFilterFactory<ServiceAuthenticationFilter.Config> {

    @Value("${service.api.key:${SERVICE_API_KEY:defaultServiceApiKeyForInternalCommunication}}")
    private String serviceApiKey;

    public ServiceAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            exchange.getRequest().mutate()
                    .header("X-Service-Auth", serviceApiKey)
                    .build();
            
            return chain.filter(exchange.mutate().request(exchange.getRequest()).build());
        };
    }

    public static class Config {
    }
}