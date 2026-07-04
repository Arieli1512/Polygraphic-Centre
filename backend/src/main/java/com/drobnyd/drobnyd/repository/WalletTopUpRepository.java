package com.drobnyd.drobnyd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.WalletTopUp;

public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Integer> {

    List<WalletTopUp> findByWallet_ClientIdOrderByCreatedAtDesc(Integer clientId);
}