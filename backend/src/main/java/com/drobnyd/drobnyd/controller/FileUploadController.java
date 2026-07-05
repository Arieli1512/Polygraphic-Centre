package com.drobnyd.drobnyd.controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.config.properties.GcsProperties;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.service.GcsSignedUrlService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    private static final long MAX_FILE_SIZE_BYTES = 30L * 1024L * 1024L;

    private final ApiResponseFactory apiResponseFactory;
    private final GcsProperties gcsProperties;
    private final GcsSignedUrlService gcsSignedUrlService;

    public FileUploadController(
            ApiResponseFactory apiResponseFactory,
            GcsProperties gcsProperties,
            GcsSignedUrlService gcsSignedUrlService) {
        this.apiResponseFactory = apiResponseFactory;
        this.gcsProperties = gcsProperties;
        this.gcsSignedUrlService = gcsSignedUrlService;
    }

    @PostMapping("/upload-requests")
    @PreAuthorize("hasRole('CLIENT')")
    public ApiSuccessResponse<UploadRequestResponse> createUploadRequest(
            Authentication authentication,
            @Valid @RequestBody CreateUploadRequest request) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Creating upload request for clientId={} file={}", requestId, user.localId(), request.fileName());

        String objectPath = buildObjectPath(user.localId(), request.fileName());
        Map<String, String> uploadHeaders = Map.of(
                "x-goog-meta-request-id", requestId,
                "x-goog-meta-client-id", String.valueOf(user.localId()));
        GcsSignedUrlService.SignedUploadUrl signedUploadUrl = gcsSignedUrlService
                .createUploadSignedUrl(objectPath, request.contentType(), uploadHeaders);

        UploadRequestResponse payload = new UploadRequestResponse(
                signedUploadUrl.url(),
                objectPath,
                request.contentType(),
                request.fileSizeBytes(),
                signedUploadUrl.expiresAt(),
                signedUploadUrl.requiredHeaders());

        return apiResponseFactory.success(payload);
    }

    private String buildObjectPath(Integer clientId, String fileName) {
        OffsetDateTime now = OffsetDateTime.now();
        String yyyy = String.valueOf(now.getYear());
        String mm = String.format("%02d", now.getMonthValue());
        String dd = String.format("%02d", now.getDayOfMonth());

        // Client-scoped prefix keeps isolation explicit and makes lifecycle policies
        // easier
        // to target.
        return gcsProperties.uploadRootPrefix()
                + "/" + clientId
                + "/orders/" + yyyy + "/" + mm + "/" + dd
                + "/" + UUID.randomUUID() + "-" + sanitizeFileName(fileName);
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private SessionUser requireSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }

        return sessionUser;
    }

    private String requestId() {
        String requestId = MDC.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown-request" : requestId;
    }

    public record CreateUploadRequest(
            @NotBlank String fileName,
            @NotBlank @Pattern(regexp = "application/pdf", message = "Only PDF uploads are supported") String contentType,
            @Min(1) @Max(MAX_FILE_SIZE_BYTES) long fileSizeBytes) {
    }

    public record UploadRequestResponse(
            String uploadUrl,
            String objectPath,
            String contentType,
            long fileSizeBytes,
            Instant expiresAt,
            Map<String, String> uploadHeaders) {
    }
}
