package com.drobnyd.drobnyd.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:5173")
                : List.copyOf(allowedOrigins);
        allowedMethods = allowedMethods == null || allowedMethods.isEmpty()
                ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                : List.copyOf(allowedMethods);
        allowedHeaders = allowedHeaders == null || allowedHeaders.isEmpty()
                ? List.of("Authorization", "Content-Type", "x-request-id", "traceparent", "tracestate",
                        "X-XSRF-TOKEN")
                : List.copyOf(allowedHeaders);
    }
}