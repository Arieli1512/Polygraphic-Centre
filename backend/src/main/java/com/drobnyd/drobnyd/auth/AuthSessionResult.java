package com.drobnyd.drobnyd.auth;

public record AuthSessionResult(
        SessionUser user,
        String accessToken,
        String refreshToken
) {
}

