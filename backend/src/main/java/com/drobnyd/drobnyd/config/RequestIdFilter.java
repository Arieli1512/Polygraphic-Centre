package com.drobnyd.drobnyd.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtr uruchamiany automatycznie przez Springa dla każdego requestu HTTP.
 *
 * Jego zadaniem jest ustawienie identyfikatora requestu:
 * - jeśli klient przysłał nagłówek x-request-id, używamy tej wartości,
 * - jeśli go nie przysłał, generujemy nowe UUID.
 *
 * requestId zapisujemy jako atrybut requestu, żeby kontrolery i obsługa błędów
 * mogły go później odczytać. Ten sam requestId zwracamy też w nagłówku
 * odpowiedzi, żeby łatwiej powiązać odpowiedź API z logami backendu.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "x-request-id";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
