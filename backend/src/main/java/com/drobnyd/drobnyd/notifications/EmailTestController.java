package com.drobnyd.drobnyd.notifications;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<Object> sendNotification(
            @RequestBody EmailRequest request,
            @RequestHeader(value = "x-request-id", required = false) String incomingRequestId) {

        String requestId = incomingRequestId != null ? incomingRequestId : UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        String timestamp = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        try {
            emailService.sendOrderStatusEmail(request.email(), request.status());

            Map<String, Object> responseData = Map.of(
                    "email", request.email(),
                    "status", request.status()
            );

            Map<String, Object> responseMeta = Map.of(
                    "requestId", requestId,
                    "timestamp", timestamp
            );

            Map<String, Object> successResponse = Map.of(
                    "data", responseData,
                    "meta", responseMeta
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);

        } catch (Exception e) {
            ProblemDetailsError problemDetails = new ProblemDetailsError(
                    "https://api.polygraphic-centre.dev/problems/internal-error",
                    "Internal Server Error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to send email: " + e.getMessage(),
                    "/api/v1/emails",
                    "INTERNAL_ERROR",
                    "Nie udało się wysłać wiadomości e-mail z powiadomieniem.",
                    "Spróbuj ponownie później lub skontaktuj się z administratorem systemu.",
                    requestId,
                    traceId,
                    timestamp
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problemDetails);
        }
    }
}

// Modele danych (DTO) poniżej można wydzielić do osobnych plików w pakiecie np. "dto"

// Model zapytania (przyjmowany w ciele żądania)
record EmailRequest(String email, String status) {}

record ProblemDetailsError(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String userMessage,
        String action,
        String requestId,
        String traceId,
        String timestamp
) {}