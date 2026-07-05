package com.drobnyd.drobnyd.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drobnyd.drobnyd.auth.SessionUser;
import com.drobnyd.drobnyd.dto.ApiResponseFactory;
import com.drobnyd.drobnyd.dto.ApiSuccessResponse;
import com.drobnyd.drobnyd.entity.Wallet;
import com.drobnyd.drobnyd.entity.WalletTopUp;
import com.drobnyd.drobnyd.exception.AuthenticationFailedException;
import com.drobnyd.drobnyd.service.BalanceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/client/wallet")
@PreAuthorize("hasRole('CLIENT')")
public class ClientWalletController {

    private static final Logger log = LoggerFactory.getLogger(ClientWalletController.class);

    private final BalanceService balanceService;
    private final ApiResponseFactory apiResponseFactory;

    public ClientWalletController(BalanceService balanceService, ApiResponseFactory apiResponseFactory) {
        this.balanceService = balanceService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    public ApiSuccessResponse<ClientWalletResponse> getWallet(Authentication authentication) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Loading wallet for client {}", requestId, user.localId());

        Wallet wallet = balanceService.getWallet(user.localId());
        List<WalletTopUpResponse> topUps = balanceService.getTopUps(user.localId()).stream()
                .map(topUp -> new WalletTopUpResponse(
                        topUp.getTopUpId(),
                        topUp.getAmount(),
                        topUp.getCreatedAt()))
                .toList();

        ClientWalletResponse payload = new ClientWalletResponse(
                wallet.getClientId(),
                wallet.getBalance(),
                wallet.getStatus().name(),
                wallet.getUpdatedAt(),
                topUps);

        return apiResponseFactory.success(payload);
    }

    @PostMapping("/top-ups")
    public ApiSuccessResponse<WalletTopUpResultResponse> topUpWallet(
            Authentication authentication,
            @Valid @RequestBody WalletTopUpRequest request) {
        SessionUser user = requireSessionUser(authentication);
        String requestId = requestId();
        log.info("[{}] Creating wallet top-up for client {} amount={}", requestId, user.localId(), request.amount());

        WalletTopUp topUp = balanceService.recordTopUp(user.localId(), request.amount());
        Wallet wallet = balanceService.getWallet(user.localId());

        WalletTopUpResultResponse payload = new WalletTopUpResultResponse(
                wallet.getClientId(),
                topUp.getTopUpId(),
                topUp.getAmount(),
                wallet.getBalance(),
                topUp.getCreatedAt());

        return apiResponseFactory.success(payload);
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

    public record WalletTopUpRequest(
            @NotNull @Min(1) Long amount) {
    }

    public record WalletTopUpResultResponse(
            Integer clientId,
            Integer topUpId,
            long amount,
            long balance,
            OffsetDateTime createdAt) {
    }

    public record WalletTopUpResponse(
            Integer topUpId,
            long amount,
            OffsetDateTime createdAt) {
    }

    public record ClientWalletResponse(
            Integer clientId,
            long balance,
            String status,
            OffsetDateTime updatedAt,
            List<WalletTopUpResponse> topUps) {
    }
}
