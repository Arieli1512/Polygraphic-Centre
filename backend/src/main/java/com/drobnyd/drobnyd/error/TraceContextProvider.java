package com.drobnyd.drobnyd.error;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TraceContextProvider {

    private final Tracer tracer;

    public TraceContextProvider(ObjectProvider<Tracer> tracerProvider) {
        this.tracer = tracerProvider.getIfAvailable();
    }

    public String currentTraceId() {
        if (tracer == null) {
            return null;
        }

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return null;
        }

        return currentSpan.context().traceId();
    }
}
