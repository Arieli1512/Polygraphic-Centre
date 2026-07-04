package com.drobnyd.drobnyd.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.drobnyd.drobnyd.exception.ApiProblemDetails;
import com.drobnyd.drobnyd.exception.ApiProblemFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ApiProblemFactory apiProblemFactory;

    public ProblemDetailsAuthenticationEntryPoint(ObjectMapper objectMapper, ApiProblemFactory apiProblemFactory) {
        this.objectMapper = objectMapper;
        this.apiProblemFactory = apiProblemFactory;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ApiProblemDetails body = apiProblemFactory.create(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "https://api.polygraphic-centre.dev/problems/authentication-required",
                "Authentication Required",
                "Authentication is required to access this resource.",
                "AUTHENTICATION_REQUIRED",
                "Ta operacja wymaga zalogowania.",
                "Zaloguj sie i sprobuj ponownie.",
                request,
                false,
                null,
                null);
        response.setStatus(body.status());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}