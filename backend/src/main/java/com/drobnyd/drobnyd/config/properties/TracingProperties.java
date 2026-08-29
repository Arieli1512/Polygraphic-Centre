package com.drobnyd.drobnyd.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracing")
public record TracingProperties(
        String requestIdHeader,
        String traceparentHeader,
        String tracestateHeader,
        String requestIdMdcKey,
        String traceIdMdcKey) {

    public TracingProperties {
        requestIdHeader = defaultString(requestIdHeader, "x-request-id");
        traceparentHeader = defaultString(traceparentHeader, "traceparent");
        tracestateHeader = defaultString(tracestateHeader, "tracestate");
        requestIdMdcKey = defaultString(requestIdMdcKey, "requestId");
        traceIdMdcKey = defaultString(traceIdMdcKey, "traceId");
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}