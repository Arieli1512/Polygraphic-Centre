package com.drobnyd.drobnyd.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.entity.Operator;
import com.drobnyd.drobnyd.entity.OperatorRole;
import com.drobnyd.drobnyd.entity.OperatorStatus;
import com.drobnyd.drobnyd.entity.PrintingPoint;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.exception.ConfigurationConflictException;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import com.drobnyd.drobnyd.repository.OperatorRepository;
import com.drobnyd.drobnyd.repository.PrintingPointRepository;
import com.drobnyd.drobnyd.service.OperatorInvitationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private static final Logger log = LoggerFactory.getLogger(AdminManagementController.class);

    private final PrintingPointRepository printingPointRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorInvitationService operatorInvitationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminManagementController(
            PrintingPointRepository printingPointRepository,
            OperatorRepository operatorRepository,
            OperatorInvitationService operatorInvitationService,
            ApiResponseFactory apiResponseFactory) {
        this.printingPointRepository = printingPointRepository;
        this.operatorRepository = operatorRepository;
        this.operatorInvitationService = operatorInvitationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/printing-points")
    public ApiSuccessResponse<List<PrintingPointResponse>> listPrintingPoints(Authentication authentication) {
        requireAdminSessionUser(authentication);
        List<PrintingPointResponse> payload = printingPointRepository.findAll().stream()
                .map(point -> new PrintingPointResponse(
                        point.getPrintingPointId(),
                        point.getName(),
                        point.getStreetAddress(),
                        point.getCity(),
                        point.getPostalCode(),
                        point.getCountry(),
                        point.getHourlyOrderLimit()))
                .toList();
        return apiResponseFactory.success(payload);
    }

    @PostMapping("/printing-points")
    public ApiSuccessResponse<PrintingPointResponse> createPrintingPoint(
            Authentication authentication,
            @Valid @RequestBody CreatePrintingPointRequest request) {
        requireAdminSessionUser(authentication);

        PrintingPoint saved = printingPointRepository.save(PrintingPoint.create(
                request.name(),
                request.streetAddress(),
                request.city(),
                request.postalCode(),
                request.country(),
                request.hourlyOrderLimit()));

        return apiResponseFactory.success(new PrintingPointResponse(
                saved.getPrintingPointId(),
                saved.getName(),
                saved.getStreetAddress(),
                saved.getCity(),
                saved.getPostalCode(),
                saved.getCountry(),
                saved.getHourlyOrderLimit()));
    }

    @PutMapping("/printing-points/{printingPointId}")
    public ApiSuccessResponse<PrintingPointResponse> updatePrintingPoint(
            Authentication authentication,
            @PathVariable Integer printingPointId,
            @Valid @RequestBody UpdatePrintingPointRequest request) {
        requireAdminSessionUser(authentication);

        PrintingPoint point = printingPointRepository.findById(printingPointId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintingPoint", printingPointId.toString()));

        point.setName(request.name());
        point.setStreetAddress(request.streetAddress());
        point.setCity(request.city());
        point.setPostalCode(request.postalCode());
        point.setCountry(request.country());
        point.setHourlyOrderLimit(request.hourlyOrderLimit());

        PrintingPoint saved = printingPointRepository.save(point);

        return apiResponseFactory.success(new PrintingPointResponse(
                saved.getPrintingPointId(),
                saved.getName(),
                saved.getStreetAddress(),
                saved.getCity(),
                saved.getPostalCode(),
                saved.getCountry(),
                saved.getHourlyOrderLimit()));
    }

    @PostMapping("/printing-points/{printingPointId}/toggle-enabled")
    public ApiSuccessResponse<SkeletonToggleResponse> togglePrintingPointEnabled(
            Authentication authentication,
            @PathVariable Integer printingPointId,
            @Valid @RequestBody ToggleEnabledRequest request) {
        requireAdminSessionUser(authentication);
        if (!printingPointRepository.existsById(printingPointId)) {
            throw new ResourceNotFoundException("PrintingPoint", printingPointId.toString());
        }

        log.info(
                "Toggle printing point availability requested for printingPointId={}, enabled={} (skeleton-only in model v0)",
                printingPointId,
                request.enabled());

        return apiResponseFactory.success(new SkeletonToggleResponse(
                printingPointId,
                request.enabled(),
                "SKELETON_ONLY",
                "Model v0 does not persist enabled/disabled state for printing points yet."));
    }

    @GetMapping("/operators")
    public ApiSuccessResponse<List<OperatorResponse>> listOperators(
            Authentication authentication,
            @RequestParam(required = false) Integer printingPointId) {
        requireAdminSessionUser(authentication);

        List<Operator> operators = printingPointId == null
                ? operatorRepository.findAllByOrderByCreatedAtDesc()
                : operatorRepository.findByPrintingPoint_PrintingPointIdOrderByCreatedAtDesc(printingPointId);

        List<OperatorResponse> payload = operators.stream()
                .map(operator -> new OperatorResponse(
                        operator.getOperatorId(),
                        operator.getPrintingPoint().getPrintingPointId(),
                        operator.getFirebaseUid(),
                        operator.getEmail(),
                        operator.getEmployeeNumber(),
                        operator.getRole(),
                        operator.getStatus(),
                        operator.getCreatedAt(),
                        operator.getUpdatedAt(),
                        "",
                        false))
                .toList();

        return apiResponseFactory.success(payload);
    }

    @PostMapping("/operators")
    public ApiSuccessResponse<OperatorResponse> createOperator(
            Authentication authentication,
            @Valid @RequestBody CreateOperatorRequest request) {
        requireAdminSessionUser(authentication);

        OperatorInvitationService.OperatorInvitationResult result = operatorInvitationService.inviteOperator(
                request.printingPointId(),
                request.email(),
                request.employeeNumber(),
                request.role());

        return apiResponseFactory.success(new OperatorResponse(
                result.operatorId(),
                result.printingPointId(),
                result.firebaseUid(),
                result.email(),
                result.employeeNumber(),
                result.role(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.inviteLink(),
                result.inviteSent()));
    }

    @PatchMapping("/operators/{operatorId}")
    public ApiSuccessResponse<OperatorResponse> updateOperator(
            Authentication authentication,
            @PathVariable Integer operatorId,
            @Valid @RequestBody UpdateOperatorRequest request) {
        requireAdminSessionUser(authentication);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator", operatorId.toString()));

        if (request.role() != null) {
            operator.setRole(request.role());
        }

        if (request.status() != null) {
            operator.setStatus(request.status());
        }

        Operator saved = operatorRepository.save(operator);

        return apiResponseFactory.success(new OperatorResponse(
                saved.getOperatorId(),
                saved.getPrintingPoint().getPrintingPointId(),
                saved.getFirebaseUid(),
                saved.getEmail(),
                saved.getEmployeeNumber(),
                saved.getRole(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                "",
                false));
    }

    private SessionUser requireAdminSessionUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser sessionUser)) {
            throw new AuthenticationFailedException("No authenticated session");
        }

        if (!"ADMIN".equalsIgnoreCase(sessionUser.role())) {
            throw new AuthenticationFailedException("Authenticated user has no ADMIN role");
        }

        return sessionUser;
    }

    public record PrintingPointResponse(
            Integer printingPointId,
            String name,
            String streetAddress,
            String city,
            String postalCode,
            String country,
            Integer hourlyOrderLimit) {
    }

    public record CreatePrintingPointRequest(
            @NotBlank String name,
            @NotBlank String streetAddress,
            @NotBlank String city,
            @NotBlank String postalCode,
            @NotBlank String country,
            @NotNull @Min(1) Integer hourlyOrderLimit) {
    }

    public record UpdatePrintingPointRequest(
            @NotBlank String name,
            @NotBlank String streetAddress,
            @NotBlank String city,
            @NotBlank String postalCode,
            @NotBlank String country,
            @NotNull @Min(1) Integer hourlyOrderLimit) {
    }

    public record ToggleEnabledRequest(
            boolean enabled) {
    }

    public record SkeletonToggleResponse(
            Integer printingPointId,
            boolean requestedEnabled,
            String mode,
            String note) {
    }

    public record OperatorResponse(
            Integer operatorId,
            Integer printingPointId,
            String firebaseUid,
            String email,
            String employeeNumber,
            OperatorRole role,
            OperatorStatus status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt,
            String inviteLink,
            boolean inviteSent) {
    }

    public record CreateOperatorRequest(
            @NotNull Integer printingPointId,
            @NotBlank @Email String email,
            @NotBlank String employeeNumber,
            @NotNull OperatorRole role) {
    }

    public record UpdateOperatorRequest(
            OperatorRole role,
            OperatorStatus status) {
    }
}
