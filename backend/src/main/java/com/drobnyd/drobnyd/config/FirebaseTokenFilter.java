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
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        if (request == null || response == null || filterChain == null) {
            return;
        }

        String headerToken = authorizationHeaderToken(request);
        String cookieToken = authCookieService.readAccessToken(request);

        if (isBlank(headerToken) && isBlank(cookieToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Prefer Authorization header if present, but fall back to cookie when header
        // contains an invalid or stale token.
        if (tryAuthenticate(request, headerToken, "authorization-header")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isBlank(headerToken) && tryAuthenticate(request, cookieToken, "access-cookie")) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
