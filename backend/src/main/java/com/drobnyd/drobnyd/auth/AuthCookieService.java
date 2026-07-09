package com.drobnyd.drobnyd.auth;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import com.drobnyd.drobnyd.config.properties.AuthProperties;

/**
 * HTTP-only session cookie management.
 * 
 * Responsible for creating and clearing session cookies (access token and
 * refresh token).
 * All cookies are httpOnly to prevent XSS attacks.
 * 
 * IMPORTANT: Cookie Domain Configuration
 * ======================================
 * 
 * When deployed behind a reverse proxy (e.g., Firebase Hosting with rewrites),
 * the explicit cookie domain MUST be configured, otherwise cookies will not be
 * sent on subsequent requests.
 * 
 * Problem: Without explicit domain, the browser defaults to "host-only"
 * cookies,
 * which are only sent when the hostname matches exactly. When Firebase Hosting
 * rewrites /api/** to Cloud Run backend, the backend sets the cookie for its
 * internal hostname. Subsequent requests from the browser go to the Firebase
 * domain, causing a mismatch and the cookie is not sent.
 * 
 * Solution: Set APP_AUTH_COOKIE_DOMAIN environment variable to a shared domain:
 * - For Firebase Hosting: APP_AUTH_COOKIE_DOMAIN=.firebaseapp.com (note the
 * dot)
 * - For specific domain: APP_AUTH_COOKIE_DOMAIN=PROJECT_ID.web.app (no dot)
 * - For local development: Leave empty (host-only is fine for localhost)
 * 
 * Reference: See gcp_instructions.md sections 14.4 and 13.6
 */
@Service
@SuppressWarnings("unused")
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "pc_access_token";
    public static final String REFRESH_COOKIE_NAME = "pc_refresh_token";
    // Firebase Hosting reliably forwards __session cookie through rewrites.
    public static final String HOSTING_SESSION_COOKIE_NAME = "__session";
    private final AuthProperties authProperties;

    public AuthCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void writeSessionCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(ACCESS_COOKIE_NAME, accessToken, accessCookieMaxAgeSeconds()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_COOKIE_NAME, refreshToken, refreshCookieMaxAgeSeconds()).toString());
        // Keep refresh token mirrored in __session for Firebase Hosting rewrites.
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(HOSTING_SESSION_COOKIE_NAME, refreshToken, refreshCookieMaxAgeSeconds()).toString());
    }

    public void clearSessionCookies(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (String path : cookiePathsToClear(request)) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE_NAME, "", 0, path).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE_NAME, "", 0, path).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                    buildCookie(HOSTING_SESSION_COOKIE_NAME, "", 0, path).toString());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("JSESSIONID", "/"));
        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("JSESSIONID", "/api"));
    }

    public String readAccessToken(HttpServletRequest request) {
        return cookieValue(request, ACCESS_COOKIE_NAME);
    }

    public String readRefreshToken(HttpServletRequest request) {
        String refresh = cookieValue(request, REFRESH_COOKIE_NAME);
        if (refresh != null && !refresh.isBlank()) {
            return refresh;
        }
        return cookieValue(request, HOSTING_SESSION_COOKIE_NAME);
    }

    private String cookieValue(HttpServletRequest request, String cookieName) {
        var cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return buildCookie(name, value, maxAgeSeconds, authProperties.cookiePath());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, String path) {
        var builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authProperties.cookieSecure())
                .path(path)
                .sameSite(authProperties.cookieSameSite())
                .maxAge(maxAgeSeconds);

        // When deployed behind a reverse proxy (e.g., Firebase Hosting rewrites),
        // explicitly set the domain so cookies are accessible across the proxy
        // boundary.
        // For development or when not specified, leave domain unset (host-only
        // cookies).
        String domain = authProperties.cookieDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    private String buildSessionCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(authProperties.cookieSecure())
                .path(path)
                .maxAge(0)
                .build()
                .toString();
    }

    private Set<String> cookiePathsToClear(HttpServletRequest request) {
        Set<String> paths = new LinkedHashSet<>();
        paths.add(normalizePath(authProperties.cookiePath()));
        paths.add("/");
        paths.add("/api");

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank()) {
            paths.add(normalizePath(contextPath));
        }

        return paths;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private long accessCookieMaxAgeSeconds() {
        return authProperties.accessTokenTtlMinutes() * 60;
    }

    private long refreshCookieMaxAgeSeconds() {
        return authProperties.refreshTokenTtlDays() * 24 * 60 * 60;
    }
}
