package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.WalletTopUp;

public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Integer> {
}