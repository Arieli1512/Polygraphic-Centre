package com.drobnyd.drobnyd.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.drobnyd.drobnyd.config.properties.AuthProperties;

@Service
public class AuthTokenService {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private final AuthProperties authProperties;

    public AuthTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(authProperties.jwtSecret());
    }

    public String createAccessToken(SessionUser user) {
        return createToken(user, Duration.ofMinutes(authProperties.accessTokenTtlMinutes()), TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(SessionUser user) {
        return createToken(user, Duration.ofDays(authProperties.refreshTokenTtlDays()), TOKEN_TYPE_REFRESH);
    }

    public DecodedJWT verifyAccessToken(String token) {
        return verifier(TOKEN_TYPE_ACCESS).verify(token);
    }

    public DecodedJWT verifyRefreshToken(String token) {
        return verifier(TOKEN_TYPE_REFRESH).verify(token);
    }

    private String createToken(SessionUser user, Duration ttl, String tokenType) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        var builder = JWT.create()
                .withIssuer(authProperties.issuer())
                .withSubject(user.firebaseUid())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .withClaim("tokenType", tokenType)
                .withClaim("localId", user.localId())
                .withClaim("firebaseUid", user.firebaseUid())
                .withClaim("email", user.email())
                .withClaim("displayName", user.displayName())
                .withClaim("accountType", user.accountType())
                .withClaim("role", user.role());
        if (user.printingPointId() != null) {
            builder.withClaim("printingPointId", user.printingPointId());
        }
        return builder.sign(algorithm());
    }

    private JWTVerifier verifier(String tokenType) {
        return JWT.require(algorithm())
                .withIssuer(authProperties.issuer())
                .withClaim("tokenType", tokenType)
                .build();
    }

    public SessionUser toSessionUser(DecodedJWT jwt) {
        Integer localId = jwt.getClaim("localId").isNull() ? null : jwt.getClaim("localId").asInt();
        Integer printingPointId = jwt.getClaim("printingPointId").isNull() ? null
                : jwt.getClaim("printingPointId").asInt();
        return new SessionUser(
                localId,
                jwt.getSubject(),
                jwt.getClaim("email").asString(),
                jwt.getClaim("displayName").asString(),
                jwt.getClaim("accountType").asString(),
                jwt.getClaim("role").asString(),
                printingPointId);
    }

}
