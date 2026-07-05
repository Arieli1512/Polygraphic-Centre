package com.drobnyd.drobnyd.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.config.properties.NotificationMailProperties;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.entity.OperatorRole;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.exception.ConfigurationConflictException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.ClientRepository;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.drobnyd.drobnyd.repository.PrintingPointRepository;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

@Service
public class OperatorInvitationService {

    private static final Logger log = LoggerFactory.getLogger(OperatorInvitationService.class);

    private final PrintingPointRepository printingPointRepository;
    private final OperatorRepository operatorRepository;
    private final ClientRepository clientRepository;
    private final JavaMailSender javaMailSender;
    private final NotificationMailProperties notificationMailProperties;

    public OperatorInvitationService(
            PrintingPointRepository printingPointRepository,
            OperatorRepository operatorRepository,
            ClientRepository clientRepository,
            JavaMailSender javaMailSender,
            NotificationMailProperties notificationMailProperties) {
        this.printingPointRepository = printingPointRepository;
        this.operatorRepository = operatorRepository;
        this.clientRepository = clientRepository;
        this.javaMailSender = javaMailSender;
        this.notificationMailProperties = notificationMailProperties;
    }

    @Transactional
    public OperatorInvitationResult inviteOperator(
            Integer printingPointId,
            String email,
            String employeeNumber,
            OperatorRole role) {
        if (operatorRepository.findByEmail(email).isPresent()) {
            throw new ConfigurationConflictException(
                    "Operator with email already exists: " + email,
                    "Nie mozna utworzyc konta personelu o tym samym adresie email.",
                    "Uzyj innego adresu albo zaktualizuj istniejace konto.");
        }

        if (clientRepository.findByEmail(email).isPresent()) {
            throw new ConfigurationConflictException(
                    "Client with email already exists: " + email,
                    "Ten adres email jest juz uzywany przez konto klienta.",
                    "Uzyj innego adresu email dla personelu.");
        }

        PrintingPoint printingPoint = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        UserRecord firebaseUser = resolveFirebaseUser(email, employeeNumber);
        boolean inviteSent = false;

        Operator saved = operatorRepository.save(Operator.create(
                printingPoint,
                firebaseUser.getUid(),
                email,
                employeeNumber,
                role));

        String inviteLink = generateInviteLink(email);

        if (notificationMailProperties.enabled()) {
            inviteSent = sendInvitationEmail(email, employeeNumber, role, printingPoint, inviteLink);
        } else {
            log.info("Mail disabled, invitation link prepared for {}: {}", email, inviteLink);
        }

        return new OperatorInvitationResult(
                saved.getOperatorId(),
                saved.getPrintingPoint().getPrintingPointId(),
                saved.getFirebaseUid(),
                saved.getEmail(),
                saved.getEmployeeNumber(),
                saved.getRole(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                inviteLink,
                inviteSent);
    }

    private UserRecord resolveFirebaseUser(String email, String employeeNumber) {
        try {
            return FirebaseAuth.getInstance().getUserByEmail(email);
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw new ConfigurationConflictException(
                        "Unable to resolve Firebase user for email: " + email,
                        "Nie udalo sie odczytac konta Firebase dla personelu.",
                        "Sprawdz konfiguracje Firebase Auth i adres email.");
            }

            try {
                return FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(UUID.randomUUID().toString())
                        .setDisplayName(employeeNumber)
                        .setDisabled(false)
                        .setEmailVerified(false));
            } catch (FirebaseAuthException createException) {
                throw new ConfigurationConflictException(
                        "Unable to create Firebase user for email: " + email,
                        "Nie udalo sie utworzyc konta Firebase dla personelu.",
                        "Sprawdz konfiguracje Firebase i adres email.");
            }
        }
    }

    private String generateInviteLink(String email) {
        try {
            return FirebaseAuth.getInstance().generatePasswordResetLink(email);
        } catch (FirebaseAuthException exception) {
            throw new ConfigurationConflictException(
                    "Unable to generate invite link for email: " + email,
                    "Nie udalo sie wygenerowac linku do ustawienia hasla.",
                    "Sprawdz konfiguracje Firebase Auth.");
        }
    }

    private boolean sendInvitationEmail(
            String email,
            String employeeNumber,
            OperatorRole role,
            PrintingPoint printingPoint,
            String inviteLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationMailProperties.fromAddress());
            message.setTo(email);
            message.setSubject(notificationMailProperties.subjectPrefix() + " Zaproszenie do konta personelu");
            message.setText(buildInviteBody(employeeNumber, role, printingPoint, inviteLink));
            javaMailSender.send(message);
            log.info("Invitation email sent to {} for employeeNumber={}", email, employeeNumber);
            return true;
        } catch (Exception exception) {
            log.warn("Failed to send invitation email to {}: {}", email, exception.getMessage());
            return false;
        }
    }

    private String buildInviteBody(
            String employeeNumber,
            OperatorRole role,
            PrintingPoint printingPoint,
            String inviteLink) {
        return "Witaj,\n\n"
                + "Utworzono dla Ciebie konto personelu w Polygraphic Centre.\n\n"
                + "Numer pracownika: " + employeeNumber + "\n"
                + "Rola: " + role + "\n"
                + "Punkt druku: " + printingPoint.getName() + "\n\n"
                + "Aby ustawic haslo i zalogowac sie, otworz link: \n"
                + inviteLink + "\n\n"
                + "Jesli nie oczekiwales tego konta, zignoruj wiadomosc.\n\n"
                + "Polygraphic Centre";
    }

    public record OperatorInvitationResult(
            Integer operatorId,
            Integer printingPointId,
            String firebaseUid,
            String email,
            String employeeNumber,
            OperatorRole role,
            com.drobnyd.drobnyd.entity.OperatorStatus status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt,
            String inviteLink,
            boolean inviteSent) {
    }
}