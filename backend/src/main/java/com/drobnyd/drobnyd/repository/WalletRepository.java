package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {
}