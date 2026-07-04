package com.drobnyd.drobnyd.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.dto.AuthenticatedUserResponse;
import com.drobnyd.drobnyd.auth.dto.FirebaseSessionExchangeRequest;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Authentication API endpoints for user login, logout, session refresh, and
 * CSRF token management.
 * 
 * Flow:
 * 1. Client calls GET /csrf to receive CSRF token in XSRF-TOKEN cookie
 * 2. Client calls POST /session with Firebase ID token and CSRF token in header
 * 3. Backend exchanges Firebase token for local session and returns user data
 * 4. Session cookies (httpOnly) are set in response
 * 5. Client can call GET /me to verify session
 * 6. When access token expires, client calls POST /refresh with refresh token
 * 7. Client calls POST /logout to clear session cookies
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Session exchange, refresh, logout, and session inspection endpoints.")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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

    /**
     * Fetch CSRF token.
     * 
     * The CSRF token is automatically set in XSRF-TOKEN cookie by Spring Security.
     * Frontend must send this token in X-XSRF-TOKEN header for state-changing
     * requests.
     * 
     * @return Empty response with XSRF-TOKEN cookie set
     */
    @GetMapping("/csrf")
    @Operation(summary = "Issue CSRF token cookie", description = "Creates the XSRF-TOKEN cookie used by the SPA for state-changing requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRF token issued")
    })
    public ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken csrfToken) {
        String requestId = requestId();
        log.info("[{}] Issuing CSRF token", requestId);
        // Trigger token creation by calling getToken()
        csrfToken.getToken();
        return ResponseEntity.ok().build();
    }

    /**
     * Exchange Firebase ID token for session cookies.
     * 
     * This is the main login endpoint. It takes a Firebase ID token from the
     * client,
     * validates it, provisions a local user account if needed, and returns httpOnly
     * session cookies (access_token and refresh_token).
     * 
     * @param request  Firebase ID token exchange request
     * @param response HTTP response to write session cookies
     * @return Authenticated user information
     * @throws ResponseStatusException 401 if Firebase token is invalid
     */
    @PostMapping("/session")
    @Operation(summary = "Exchange Firebase ID token for local session", description = "Validates a Firebase token, provisions a local account if needed, and returns authenticated user data while setting httpOnly session cookies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session created", content = @Content(schema = @Schema(implementation = AuthenticatedUserResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/ConflictProblem"),
            @ApiResponse(responseCode = "422", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerErrorProblem")
    })
    public AuthenticatedUserResponse createSession(
            @Valid @RequestBody FirebaseSessionExchangeRequest request,
            HttpServletResponse response) {
        String requestId = requestId();
        log.info("[{}] Exchanging Firebase token for session", requestId);

        try {
            AuthSessionResult session = authService.exchangeFirebaseToken(request.idToken());
            authCookieService.writeSessionCookies(response, session.accessToken(), session.refreshToken());

            SessionUser user = session.user();
            log.info("[{}] Session created for user: {} ({})", requestId, user.displayName(), user.accountType());

            return user.toResponse();
        } catch (Exception e) {
            log.warn("[{}] Failed to exchange Firebase token: {}", requestId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get current authenticated user information.
     * 
     * Requires valid access token in Authorization header or pc_access_token
     * cookie.
     * 
     * @param authentication Spring Security authentication object (populated by
     *                       FirebaseTokenFilter)
     * @return Current user information
     * @throws ResponseStatusException 401 if no valid authentication
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user", description = "Returns the user profile represented by the active access cookie.", security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user loaded", content = @Content(schema = @Schema(implementation = AuthenticatedUserResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedProblem"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedProblem")
    })
    public AuthenticatedUserResponse me(Authentication authentication) {
        String requestId = requestId();

        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            log.warn("[{}] GET /me called without valid authentication", requestId);
            throw new AuthenticationFailedException("No authenticated session");
        }

        log.info("[{}] User {} retrieved session info", requestId, sessionUser.displayName());
        return sessionUser.toResponse();
    }

    /**
     * Refresh access token using refresh token.
     * 
     * When access token expires (after 15 minutes), client calls this endpoint with
     * the refresh token (from pc_refresh_token cookie) to get a new access token.
     * 
     * @param request  HTTP request containing pc_refresh_token cookie
     * @param response HTTP response to write new session cookies
     * @return Updated user information
     * @throws ResponseStatusException 401 if refresh token is missing or invalid
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses the refresh cookie to issue a fresh access cookie and return current user data.", security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed", content = @Content(schema = @Schema(implementation = AuthenticatedUserResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerErrorProblem")
    })
    public AuthenticatedUserResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String requestId = requestId();
        log.info("[{}] Refreshing access token", requestId);

        String refreshToken = authCookieService.readRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("[{}] Refresh attempted without valid refresh token", requestId);
            throw new AuthenticationFailedException("Missing or invalid refresh token");
        }

        try {
            SessionUser user = authService.refreshSession(refreshToken);
            AuthSessionResult refreshed = new AuthSessionResult(
                    user,
                    authTokenService.createAccessToken(user),
                    authTokenService.createRefreshToken(user));
            authCookieService.writeSessionCookies(response, refreshed.accessToken(), refreshed.refreshToken());

            log.info("[{}] Access token refreshed for user: {}", requestId, user.displayName());
            return refreshed.user().toResponse();
        } catch (Exception e) {
            log.warn("[{}] Token refresh failed: {}", requestId, e.getMessage());
            throw new AuthenticationFailedException("Invalid refresh token", e);
        }
    }

    /**
     * Logout user by clearing session cookies.
     * 
     * Removes both access_token and refresh_token httpOnly cookies.
     * 
    * @param request HTTP request used to invalidate any existing servlet session
     * @param response HTTP response to clear cookies
     * @return 204 No Content
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and clear session cookies", description = "Clears access and refresh cookies for the active browser session.", security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session cleared"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedProblem")
    })
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String requestId = requestId();
        log.info("[{}] User logout", requestId);
        authCookieService.clearSessionCookies(request, response);
        return ResponseEntity.noContent().build();
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }
}
