package com.drobnyd.drobnyd.config;

import java.io.IOException;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.drobnyd.drobnyd.auth.AuthCookieService;
import com.drobnyd.drobnyd.auth.AuthTokenService;
import com.drobnyd.drobnyd.auth.SessionUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    private final AuthCookieService authCookieService;
    private final AuthTokenService authTokenService;

    public FirebaseTokenFilter(AuthCookieService authCookieService, AuthTokenService authTokenService) {
        this.authCookieService = authCookieService;
        this.authTokenService = authTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/webhooks/pubsub/");
    }

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        if (request == null || response == null || filterChain == null) {
            return;
        }

        String headerToken = authorizationHeaderToken(request);
        String cookieToken = authCookieService.readAccessToken(request);
        String refreshCookieToken = authCookieService.readRefreshToken(request);

        if (log.isDebugEnabled()) {
            log.debug(
                    "Auth probe {} {} host={} origin={} authHeaderPresent={} accessCookiePresent={} refreshCookiePresent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getHeader("Host"),
                    request.getHeader("Origin"),
                    !isBlank(headerToken),
                    !isBlank(cookieToken),
                    !isBlank(refreshCookieToken));
        }

        if (isBlank(headerToken) && isBlank(cookieToken) && isBlank(refreshCookieToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Prefer Authorization header if present, but fall back to cookie when header
        // contains an invalid or stale token.
        if (tryAuthenticate(request, headerToken, "authorization-header")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tryAuthenticate(request, cookieToken, "access-cookie")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Resilience fallback for production proxy/cookie edge-cases:
        // if access token is missing/invalid but refresh token is valid, allow
        // authentication using refresh token claims so protected endpoints do not
        // enter a 401->refresh infinite loop in the SPA.
        if (tryAuthenticateWithRefreshToken(request, refreshCookieToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    private String authorizationHeaderToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private boolean tryAuthenticate(HttpServletRequest request, String token, String tokenSource) {
        if (isBlank(token)) {
            return false;
        }

        try {
            SessionUser sessionUser = authTokenService.toSessionUser(authTokenService.verifyAccessToken(token));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    sessionUser,
                    null,
                    sessionUser.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (JWTVerificationException exception) {
            log.warn("Authentication token rejected from {} for {} {}: {}",
                    tokenSource,
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage());
            return false;
        }
    }

    private boolean tryAuthenticateWithRefreshToken(HttpServletRequest request, String refreshToken) {
        if (isBlank(refreshToken)) {
            return false;
        }

        try {
            SessionUser sessionUser = authTokenService.toSessionUser(authTokenService.verifyRefreshToken(refreshToken));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    sessionUser,
                    null,
                    sessionUser.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.warn("Authenticated via refresh-cookie fallback for {} {}. Access token was missing or invalid.",
                    request.getMethod(), request.getRequestURI());
            return true;
        } catch (JWTVerificationException exception) {
            log.warn("Refresh token rejected for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
