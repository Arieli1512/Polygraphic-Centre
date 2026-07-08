package com.drobnyd.drobnyd.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        String issuer,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        String cookiePath,
        boolean cookieSecure,
        String cookieSameSite,
        String cookieDomain) {

    public AuthProperties {
        jwtSecret = defaultString(jwtSecret, "polygraphic-centre-dev-secret");
        issuer = defaultString(issuer, "polygraphic-centre");
        accessTokenTtlMinutes = accessTokenTtlMinutes <= 0 ? 15 : accessTokenTtlMinutes;
        refreshTokenTtlDays = refreshTokenTtlDays <= 0 ? 7 : refreshTokenTtlDays;
        cookiePath = defaultString(cookiePath, "/");
        cookieDomain = defaultString(cookieDomain, null);
        cookieSameSite = defaultString(cookieSameSite, "Lax");
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}