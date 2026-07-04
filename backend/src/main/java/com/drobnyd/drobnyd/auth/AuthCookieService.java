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

@Service
@SuppressWarnings("unused")
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "pc_access_token";
    public static final String REFRESH_COOKIE_NAME = "pc_refresh_token";
    private final AuthProperties authProperties;

    public AuthCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void writeSessionCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(ACCESS_COOKIE_NAME, accessToken, accessCookieMaxAgeSeconds()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_COOKIE_NAME, refreshToken, refreshCookieMaxAgeSeconds()).toString());
    }

    public void clearSessionCookies(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (String path : cookiePathsToClear(request)) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE_NAME, "", 0, path).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE_NAME, "", 0, path).toString());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("JSESSIONID", "/"));
        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("JSESSIONID", "/api"));
    }

    public String readAccessToken(HttpServletRequest request) {
        return cookieValue(request, ACCESS_COOKIE_NAME);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return cookieValue(request, REFRESH_COOKIE_NAME);
    }

    private String cookieValue(HttpServletRequest request, String cookieName) {
        var cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return buildCookie(name, value, maxAgeSeconds, authProperties.cookiePath());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authProperties.cookieSecure())
                .path(path)
                .sameSite(authProperties.cookieSameSite())
                .maxAge(maxAgeSeconds)
                .build();
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
