package com.drobnyd.drobnyd.auth;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.drobnyd.drobnyd.entity.Client;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

@Service
public class AuthService {

    private final ClientRepository clientRepository;
    private final OperatorRepository operatorRepository;
    private final AuthTokenService authTokenService;

    public AuthService(
            ClientRepository clientRepository,
            OperatorRepository operatorRepository,
            AuthTokenService authTokenService) {
        this.clientRepository = clientRepository;
        this.operatorRepository = operatorRepository;
        this.authTokenService = authTokenService;
    }

    @Transactional
    public AuthSessionResult exchangeFirebaseToken(String idToken) {
        FirebaseToken firebaseToken = verifyFirebaseToken(idToken);
        SessionUser user = resolveOrProvisionSessionUser(firebaseToken);
        return createSession(user);
    }

    @Transactional(readOnly = true)
    public SessionUser refreshSession(String refreshToken) {
        SessionUser tokenUser = authTokenService.toSessionUser(authTokenService.verifyRefreshToken(refreshToken));
        return resolveExistingSessionUser(tokenUser.firebaseUid());
    }

    @Transactional(readOnly = true)
    public SessionUser resolveSessionUser(String accessToken) {
        SessionUser tokenUser = authTokenService.toSessionUser(authTokenService.verifyAccessToken(accessToken));
        return resolveExistingSessionUser(tokenUser.firebaseUid());
    }

    private AuthSessionResult createSession(SessionUser user) {
        String accessToken = authTokenService.createAccessToken(user);
        String refreshToken = authTokenService.createRefreshToken(user);
        return new AuthSessionResult(user, accessToken, refreshToken);
    }

    private FirebaseToken verifyFirebaseToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Firebase ID token", exception);
        }
    }

    private SessionUser resolveOrProvisionSessionUser(FirebaseToken firebaseToken) {
        String firebaseUid = firebaseToken.getUid();
        String email = safeEmail(firebaseToken, firebaseUid);
        String displayName = resolveDisplayName(firebaseToken, email);

        Optional<Client> client = clientRepository.findByFirebaseUid(firebaseUid);
        if (client.isPresent()) {
            return toClientSessionUser(client.get());
        }

        Optional<Operator> operator = operatorRepository.findByFirebaseUid(firebaseUid);
        if (operator.isPresent()) {
            return toOperatorSessionUser(operator.get());
        }

        String[] names = splitDisplayName(displayName);
        Client savedClient = clientRepository.save(Client.provisioned(firebaseUid, email, names[0], names[1]));
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

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Local account not linked to Firebase user");
    }

    private SessionUser toClientSessionUser(Client client) {
        String displayName = client.getFirstName() + " " + client.getLastName();
        return new SessionUser(
                client.getClientId(),
                client.getFirebaseUid(),
                client.getEmail(),
                displayName,
                "CLIENT",
                "CLIENT",
                null
        );
    }

    private SessionUser toOperatorSessionUser(Operator operator) {
        String displayName = operator.getEmployeeNumber();
        Integer printingPointId = operator.getPrintingPoint() == null ? null : operator.getPrintingPoint().getPrintingPointId();
        return new SessionUser(
                operator.getOperatorId(),
                operator.getFirebaseUid(),
                operator.getEmail(),
                displayName,
                "OPERATOR",
                operator.getRole().name(),
                printingPointId
        );
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

    private String[] splitDisplayName(String displayName) {
        String normalized = displayName.replace('_', ' ').replace('.', ' ').trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return new String[] { "Nowy", "Klient" };
        }
        if (parts.length == 1) {
            return new String[] { capitalize(parts[0]), "Klient" };
        }
        return new String[] { capitalize(parts[0]), capitalize(parts[1]) };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Klient";
        }
        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }
}





