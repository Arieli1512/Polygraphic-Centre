package com.drobnyd.drobnyd.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

import com.drobnyd.drobnyd.auth.dto.AuthenticatedUserResponse;
import com.drobnyd.drobnyd.auth.dto.FirebaseSessionExchangeRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokenService;
    private final AuthCookieService authCookieService;

    public AuthController(
            AuthService authService,
            AuthTokenService authTokenService,
            AuthCookieService authCookieService) {
        this.authService = authService;
        this.authTokenService = authTokenService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session")
    public AuthenticatedUserResponse createSession(
            @Valid @RequestBody FirebaseSessionExchangeRequest request,
            HttpServletResponse response) {
        AuthSessionResult session = authService.exchangeFirebaseToken(request.idToken());
        authCookieService.writeSessionCookies(response, session.accessToken(), session.refreshToken());
        return session.user().toResponse();
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(Authentication authentication) {
        return toResponse(authentication);
    }

    @PostMapping("/refresh")
    public AuthenticatedUserResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieService.readRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Missing refresh token");
        }

        SessionUser user = authService.refreshSession(refreshToken);
        AuthSessionResult refreshed = new AuthSessionResult(
                user,
                authTokenService.createAccessToken(user),
                authTokenService.createRefreshToken(user));
        authCookieService.writeSessionCookies(response, refreshed.accessToken(), refreshed.refreshToken());
        return refreshed.user().toResponse();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearSessionCookies(response);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUserResponse toResponse(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "No authenticated session");
        }
        return sessionUser.toResponse();
    }
}



