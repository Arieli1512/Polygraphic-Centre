package com.drobnyd.drobnyd.config;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.drobnyd.drobnyd.config.properties.CorsProperties;
import com.drobnyd.drobnyd.config.properties.TracingProperties;

/**
 * Spring Security configuration for authentication, authorization, CSRF
 * protection, and CORS.
 * 
 * Key architectural decisions:
 * - Session management: IF_REQUIRED to support httpOnly session cookies
 * (access/refresh tokens)
 * - CSRF protection: Enabled via CookieCsrfTokenRepository (XSRF-TOKEN in
 * readable cookie, X-XSRF-TOKEN header required)
 * - Token storage: httpOnly cookies for access_token and refresh_token (XSS
 * protection)
 * - Token verification: FirebaseTokenFilter reads tokens from cookies or
 * Authorization header
 * - Authorization endpoints (/auth/*) are permitted for all to enable
 * login/logout without auth
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final FirebaseTokenFilter firebaseTokenFilter;
    private final RequestIdFilter requestIdFilter;
    private final ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailsAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;
    private final TracingProperties tracingProperties;

    public SecurityConfig(
            FirebaseTokenFilter firebaseTokenFilter,
            RequestIdFilter requestIdFilter,
            ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
            ProblemDetailsAccessDeniedHandler accessDeniedHandler,
            CorsProperties corsProperties,
            TracingProperties tracingProperties) {
        this.firebaseTokenFilter = firebaseTokenFilter;
        this.requestIdFilter = requestIdFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsProperties = corsProperties;
        this.tracingProperties = tracingProperties;
    }

    /**
     * Register RequestIdFilter as the first servlet filter (before Spring
     * Security).
     * 
     * This ensures request ID is available for all logging and tracing.
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Integer.MIN_VALUE); // Highest priority - run first
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security filter chain: CSRF protection enabled, session creation IF_REQUIRED");

        http
                // CSRF Protection: XSRF-TOKEN in readable cookie (safe for SPA), validated via
                // X-XSRF-TOKEN header
                // This allows frontend JavaScript to read the token and send it back
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

                // CORS: Allow frontend to communicate with backend
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Session Management: IF_REQUIRED allows Spring Security to create session and
                // manage cookies
                // This is necessary for httpOnly session cookies to function properly
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints for guest users
                        .requestMatchers("/api/public/**").permitAll()
                        // Auth endpoints (login, logout, session exchange, CSRF token) - no auth
                        // required
                        .requestMatchers(
                                "/api/auth/csrf", // Fetch CSRF token
                                "/api/auth/session", // Exchange Firebase token for session
                                "/api/auth/refresh", // Refresh access token
                                "/api/auth/logout" // Logout and clear cookies
                        ).permitAll()
                        // Swagger/OpenAPI documentation (optional, can be protected)
                        .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated())

                // Inject Firebase token validator before Spring's default authentication filter
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration for SPA-to-backend communication.
     * Frontend running on localhost:5173 can make credentialed requests.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());
        configuration.setAllowCredentials(corsProperties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configured for origins: {} with tracing headers: [{}, {}, {}]",
                corsProperties.allowedOrigins(),
                tracingProperties.requestIdHeader(),
                tracingProperties.traceparentHeader(),
                tracingProperties.tracestateHeader());
        return source;
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}