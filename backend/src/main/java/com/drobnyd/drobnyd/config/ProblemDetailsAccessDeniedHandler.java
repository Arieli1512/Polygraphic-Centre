package com.drobnyd.drobnyd.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.drobnyd.drobnyd.exception.ApiProblemDetails;
import com.drobnyd.drobnyd.exception.ApiProblemFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ApiProblemFactory apiProblemFactory;

    public ProblemDetailsAccessDeniedHandler(ObjectMapper objectMapper, ApiProblemFactory apiProblemFactory) {
        this.objectMapper = objectMapper;
        this.apiProblemFactory = apiProblemFactory;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ApiProblemDetails body = apiProblemFactory.create(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "https://api.polygraphic-centre.dev/problems/access-denied",
                "Access Denied",
                "Access denied for this resource.",
                "ACCESS_DENIED",
                "Nie masz uprawnien do wykonania tej operacji.",
                "Zaloguj sie na konto z odpowiednia rola lub skontaktuj sie z administratorem.",
                request,
                false,
                null,
                null);
        response.setStatus(body.status());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}