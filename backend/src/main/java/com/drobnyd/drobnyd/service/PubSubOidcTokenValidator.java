package com.drobnyd.drobnyd.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.drobnyd.drobnyd.config.properties.PubSubProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class PubSubOidcTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(PubSubOidcTokenValidator.class);

    private final PubSubProperties pubSubProperties;
    private final GoogleIdTokenVerifier verifier;

    public PubSubOidcTokenValidator(PubSubProperties pubSubProperties) {
        this.pubSubProperties = pubSubProperties;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .build();
    }

    public boolean isValidBearerToken(String authorizationHeader) {
        if (!pubSubProperties.requireOidc()) {
            return true;
        }

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Pub/Sub push request rejected: missing bearer token");
            return false;
        }

        String token = authorizationHeader.substring("Bearer ".length());
        try {
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken == null) {
                log.warn("Pub/Sub push request rejected: token verification returned null");
                return false;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String issuer = payload.getIssuer();
            if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
                log.warn("Pub/Sub push request rejected: unexpected issuer={}", issuer);
                return false;
            }

            String expectedAudience = pubSubProperties.webhookAudience();
            if (expectedAudience != null && !expectedAudience.isBlank()) {
                List<String> audiences = payload.getAudienceAsList();
                if (audiences == null || audiences.stream().noneMatch(expectedAudience::equals)) {
                    log.warn("Pub/Sub push request rejected: audience mismatch");
                    return false;
                }
            }

            return true;
        } catch (Exception ex) {
            log.warn("Pub/Sub push request rejected: token parsing failed", ex);
            return false;
        }
    }
}
