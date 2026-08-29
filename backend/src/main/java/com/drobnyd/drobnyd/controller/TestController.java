package com.drobnyd.drobnyd.controller;

import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.dto.MessageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Diagnostics", description = "Utility endpoints used to validate security and API contracts.")
public class TestController {

    private final ApiResponseFactory apiResponseFactory;

    public TestController(ApiResponseFactory apiResponseFactory) {
        this.apiResponseFactory = apiResponseFactory;
    }

    // Anyone can access this
    @GetMapping("/public/hello")
    @Operation(summary = "Public hello endpoint", description = "Returns a minimal success envelope without authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hello payload returned", content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class)))
    })
    public ApiSuccessResponse<MessageResponse> publicHello() {
        return apiResponseFactory.success(new MessageResponse(
                "Hello from a public endpoint! No login required."));
    }

    // ONLY logged-in users with a valid token can access this
    @GetMapping("/private/dashboard")
    @Secured({ "ROLE_CLIENT", "ROLE_OPERATOR", "ROLE_ADMIN", "ROLE_EMPLOYEE" })
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Protected dashboard probe", description = "Returns a success envelope only for authenticated users with allowed roles.", security = {
            @SecurityRequirement(name = "cookieAuth")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard payload returned", content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedProblem"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccessDeniedProblem")
    })
    public ApiSuccessResponse<MessageResponse> privateDashboard() {
        return apiResponseFactory.success(new MessageResponse(
                "Welcome to the hidden dashboard! Your Firebase token is valid."));
    }
}
