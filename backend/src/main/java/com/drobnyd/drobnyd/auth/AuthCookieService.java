package com.drobnyd.drobnyd.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
@SuppressWarnings("unused")
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "pc_access_token";
    public static final String REFRESH_COOKIE_NAME = "pc_refresh_token";

    @Value("${app.auth.cookie-path:/}")
    private String cookiePath;

    @Value("${app.auth.cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.auth.cookie-same-site:Lax}")
    private String sameSite;

    @Value("${app.auth.access-token-ttl-minutes:15}")
    private long accessTokenTtlMinutes;

    @Value("${app.auth.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    public void writeSessionCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE_NAME, accessToken, accessCookieMaxAgeSeconds()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE_NAME, refreshToken, refreshCookieMaxAgeSeconds()).toString());
    }

    public void clearSessionCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE_NAME, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE_NAME, "", 0).toString());
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
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private long accessCookieMaxAgeSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    private long refreshCookieMaxAgeSeconds() {
        return refreshTokenTtlDays * 24 * 60 * 60;
    }
}


