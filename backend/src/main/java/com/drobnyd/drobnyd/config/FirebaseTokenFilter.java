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


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final AuthCookieService authCookieService;
    private final AuthTokenService authTokenService;

    public FirebaseTokenFilter(AuthCookieService authCookieService, AuthTokenService authTokenService) {
        this.authCookieService = authCookieService;
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        if (request == null || response == null || filterChain == null) {
            return;
        }

        String accessToken = resolveAccessToken(request);
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            SessionUser sessionUser = authTokenService.toSessionUser(authTokenService.verifyAccessToken(accessToken));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    sessionUser,
                    null,
                    sessionUser.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JWTVerificationException exception) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authCookieService.readAccessToken(request);
    }
}

