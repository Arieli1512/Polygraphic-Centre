package com.drobnyd.drobnyd.auth;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Client;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.entity.Wallet;
import com.drobnyd.drobnyd.auth.dto.FirebaseSessionExchangeRequest;
import com.drobnyd.drobnyd.exception.AccountLinkException;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.drobnyd.drobnyd.repository.WalletRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

/**
 * Service for Firebase authentication and local session management.
 * 
 * Handles:
 * - Firebase ID token verification
 * - Automatic provisioning of Client accounts for first-time Firebase users
 * - Session creation with JWT access and refresh tokens
 * - Token refresh
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ClientRepository clientRepository;
    private final OperatorRepository operatorRepository;
    private final WalletRepository walletRepository;
    private final AuthTokenService authTokenService;

    public AuthService(
            ClientRepository clientRepository,
            OperatorRepository operatorRepository,
            WalletRepository walletRepository,
            AuthTokenService authTokenService) {
        this.clientRepository = clientRepository;
        this.operatorRepository = operatorRepository;
        this.walletRepository = walletRepository;
        this.authTokenService = authTokenService;
    }

    /**
     * Exchange a Firebase ID token for local session.
     * 
     * Process:
     * 1. Verify Firebase ID token with Firebase Admin SDK
     * 2. Resolve or provision local user (Client or Operator)
     * 3. Create JWT access and refresh tokens
     * 
     * @param idToken Firebase ID token from client
     * @return Session result with tokens and user info
     * @throws ResponseStatusException 401 if token is invalid
     */
    @Transactional
    public AuthSessionResult exchangeFirebaseToken(FirebaseSessionExchangeRequest request) {
        FirebaseToken firebaseToken = verifyFirebaseToken(request.idToken());
        SessionUser user = resolveOrProvisionSessionUser(firebaseToken, request.firstName(), request.lastName());
        return createSession(user);
    }

    /**
     * Refresh access token using refresh token.
     * 
     * @param refreshToken Refresh token (from cookie or header)
     * @return Session user for new token generation
     * @throws ResponseStatusException 401 if refresh token is invalid
     */
    @Transactional(readOnly = true)
    public SessionUser refreshSession(String refreshToken) {
        SessionUser tokenUser = authTokenService.toSessionUser(authTokenService.verifyRefreshToken(refreshToken));
        return resolveExistingSessionUser(tokenUser.firebaseUid());
    }

    /**
     * Resolve session user from access token (for verification).
     * 
     * @param accessToken Access token to verify
     * @return Session user
     * @throws ResponseStatusException 401 if access token is invalid
     */
    @Transactional(readOnly = true)
    public SessionUser resolveSessionUser(String accessToken) {
        SessionUser tokenUser = authTokenService.toSessionUser(authTokenService.verifyAccessToken(accessToken));
        return resolveExistingSessionUser(tokenUser.firebaseUid());
    }

    private AuthSessionResult createSession(SessionUser user) {
        String accessToken = authTokenService.createAccessToken(user);
        String refreshToken = authTokenService.createRefreshToken(user);
        log.debug("Session created for user {} with ID {} (TTL: 15 min access, 7 days refresh)",
                user.displayName(), user.localId());
        return new AuthSessionResult(user, accessToken, refreshToken);
    }

    private FirebaseToken verifyFirebaseToken(String idToken) {
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            log.debug("Firebase ID token verified for user: {}", token.getUid());
            return token;
        } catch (FirebaseAuthException exception) {
            log.warn("Firebase ID token verification failed: {}", exception.getMessage());
            throw new AuthenticationFailedException("Invalid Firebase ID token", exception);
        }
    }

    private SessionUser resolveOrProvisionSessionUser(
            FirebaseToken firebaseToken,
            String requestedFirstName,
            String requestedLastName) {
        String firebaseUid = firebaseToken.getUid();
        String email = safeEmail(firebaseToken, firebaseUid);

        // Try to find existing Client
        Optional<Client> client = clientRepository.findByFirebaseUid(firebaseUid);
        if (client.isPresent()) {
            log.debug("Existing Client found for Firebase UID: {}", firebaseUid);
            return toClientSessionUser(client.get());
        }

        // Try to find existing Operator
        Optional<Operator> operator = operatorRepository.findByFirebaseUid(firebaseUid);
        if (operator.isPresent()) {
            log.debug("Existing Operator found for Firebase UID: {}", firebaseUid);
            return toOperatorSessionUser(operator.get());
        }

        // Provision new Client account
        log.info("Provisioning new Client account for Firebase UID: {} (email: {})", firebaseUid, email);
        NameParts names = resolveClientNames(firebaseToken, email, requestedFirstName, requestedLastName);
        Client savedClient = clientRepository
                .save(Client.provisioned(firebaseUid, email, names.firstName(), names.lastName()));
        walletRepository.save(Wallet.initialize(savedClient));
        log.info("New Client account created with ID: {}", savedClient.getClientId());
        return toClientSessionUser(savedClient);
    }

    private SessionUser resolveExistingSessionUser(String firebaseUid) {
        Optional<Client> client = clientRepository.findByFirebaseUid(firebaseUid);
        if (client.isPresent()) {
            return toClientSessionUser(client.get());
        }

        Optional<Operator> operator = operatorRepository.findByFirebaseUid(firebaseUid);
        if (operator.isPresent()) {
            return toOperatorSessionUser(operator.get());
        }

        log.warn("User resolution failed for Firebase UID: {} - no linked account found", firebaseUid);
        throw new AccountLinkException("Local account is not linked to the Firebase user.");
    }

    private SessionUser toClientSessionUser(Client client) {
        String displayName = (client.getFirstName() + " " + client.getLastName()).trim();
        return new SessionUser(
                client.getClientId(),
                client.getFirebaseUid(),
                client.getEmail(),
                displayName,
                "CLIENT",
                "CLIENT",
                null);
    }

    private SessionUser toOperatorSessionUser(Operator operator) {
        String displayName = operator.getEmployeeNumber();
        Integer printingPointId = operator.getPrintingPoint() == null ? null
                : operator.getPrintingPoint().getPrintingPointId();
        return new SessionUser(
                operator.getOperatorId(),
                operator.getFirebaseUid(),
                operator.getEmail(),
                displayName,
                "OPERATOR",
                operator.getRole().name(),
                printingPointId);
    }

    private String safeEmail(FirebaseToken firebaseToken, String firebaseUid) {
        String email = firebaseToken.getEmail();
        if (email != null && !email.isBlank()) {
            return email;
        }
        return firebaseUid + "@firebase.local";
    }

    private String resolveDisplayName(FirebaseToken firebaseToken, String email) {
        String name = firebaseToken.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email.substring(0, email.indexOf('@'));
    }

    private NameParts resolveClientNames(
            FirebaseToken firebaseToken,
            String email,
            String requestedFirstName,
            String requestedLastName) {
        String firstName = normalizeNamePart(requestedFirstName);
        String lastName = normalizeNamePart(requestedLastName);

        if (firstName != null || lastName != null) {
            return new NameParts(
                    firstName != null ? firstName : fallbackFirstName(firebaseToken, email),
                    lastName != null ? lastName : "");
        }

        String displayName = resolveDisplayName(firebaseToken, email);
        return splitDisplayName(displayName);
    }

    private NameParts splitDisplayName(String displayName) {
        String normalized = displayName.replace('_', ' ').replace('.', ' ').trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return new NameParts("Nowy", "");
        }
        if (parts.length == 1) {
            return new NameParts(capitalize(parts[0]), "");
        }
        return new NameParts(capitalize(parts[0]), capitalize(parts[1]));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private String normalizeNamePart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return capitalize(value);
    }

    private String fallbackFirstName(FirebaseToken firebaseToken, String email) {
        String displayName = resolveDisplayName(firebaseToken, email);
        String normalized = displayName.replace('_', ' ').replace('.', ' ').trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "Nowy";
        }
        return capitalize(parts[0]);
    }

    private record NameParts(String firstName, String lastName) {
    }
}
