package com.drobnyd.drobnyd.config;

import java.io.IOException;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.drobnyd.drobnyd.config.properties.TracingProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Request ID filter for distributed request tracing.
 * 
 * Extracts or generates a unique request ID and stores it in MDC (Mapped
 * Diagnostic Context)
 * so it's automatically included in all log entries for this request.
 * 
 * Flow:
 * 1. Check if x-request-id header exists
 * 2. If not, generate a UUID
 * 3. Store in MDC for logging
 * 4. Add to response header for client reference
 * 5. Clean up after request
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private final TracingProperties tracingProperties;

    public RequestIdFilter(TracingProperties tracingProperties) {
        this.tracingProperties = tracingProperties;
    }

    @Override
    protected void doFilterInternal(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain) throws ServletException, IOException {

        if (request == null || response == null || filterChain == null) {
            return;
        }

        // Extract or generate request ID
        String requestId = extractRequestId(request);
        @Nullable
        String traceId = extractTraceId(request);

        // Store in MDC for logging
        MDC.put(tracingProperties.requestIdMdcKey(), requestId);
        if (traceId != null) {
            MDC.put(tracingProperties.traceIdMdcKey(), traceId);
        }

        // Add to response so client can reference it
        response.setHeader(tracingProperties.requestIdHeader(), requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC
            MDC.remove(tracingProperties.requestIdMdcKey());
            MDC.remove(tracingProperties.traceIdMdcKey());
        }
    }

    /**
     * Extract request ID from header or generate new one.
     */
    private String extractRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(tracingProperties.requestIdHeader());

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        return requestId;
    }

    private @Nullable String extractTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(tracingProperties.traceparentHeader());
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }

        String[] parts = traceparent.split("-");
        if (parts.length != 4) {
            return null;
        }

        String traceId = parts[1];
        return traceId.isBlank() ? null : traceId;
    }
}
