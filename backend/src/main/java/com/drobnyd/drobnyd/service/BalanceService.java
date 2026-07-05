package com.drobnyd.drobnyd.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Wallet;
import com.drobnyd.drobnyd.entity.WalletStatus;
import com.drobnyd.drobnyd.entity.WalletTopUp;
import com.drobnyd.drobnyd.exception.InsufficientBalanceException;
import com.drobnyd.drobnyd.exception.WalletUnavailableException;
import com.drobnyd.drobnyd.repository.WalletRepository;
import com.drobnyd.drobnyd.repository.WalletTopUpRepository;
import com.drobnyd.drobnyd.service.model.BalanceCheckResult;

@Service
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);
    private final WalletRepository walletRepository;
    private final WalletTopUpRepository walletTopUpRepository;

    public BalanceService(WalletRepository walletRepository, WalletTopUpRepository walletTopUpRepository) {
        this.walletRepository = walletRepository;
        this.walletTopUpRepository = walletTopUpRepository;
    }

    @Transactional(readOnly = true)
    public BalanceCheckResult verifyBalance(Integer clientId, long requiredAmount) {
        Wallet wallet = requireActiveWallet(clientId);
        return new BalanceCheckResult(
                clientId,
                wallet.getBalance(),
                requiredAmount,
                wallet.getBalance() >= requiredAmount);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Integer clientId) {
        return requireActiveWallet(clientId);
    }

    @Transactional(readOnly = true)
    public List<WalletTopUp> getTopUps(Integer clientId) {
        requireActiveWallet(clientId);
        return walletTopUpRepository.findByWallet_ClientIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional
    public Wallet debitWallet(Integer clientId, long amount) {
        Wallet wallet = requireActiveWallet(clientId);
        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException(clientId, amount, wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance() - amount);
        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Debited wallet for client {} by {}. New balance={}", clientId, amount, savedWallet.getBalance());
        return savedWallet;
    }

    @Transactional
    public WalletTopUp recordTopUp(Integer clientId, long amount) {
        Wallet wallet = requireActiveWallet(clientId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        WalletTopUp savedTopUp = walletTopUpRepository.save(WalletTopUp.recordFor(wallet, amount));

        log.info("Recorded wallet top-up for client {} by {}. New balance={}", clientId, amount, wallet.getBalance());
        return savedTopUp;
    }

    private Wallet requireActiveWallet(Integer clientId) {
        Wallet wallet = walletRepository.findById(clientId)
                .orElseThrow(() -> new WalletUnavailableException(clientId, "wallet record is missing"));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletUnavailableException(clientId, "wallet status is " + wallet.getStatus());
        }
        return wallet;
    }
}