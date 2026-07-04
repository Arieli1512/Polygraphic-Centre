package com.drobnyd.drobnyd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.PrintSettings;

public interface PrintSettingsRepository extends JpaRepository<PrintSettings, Integer> {

    Optional<PrintSettings> findByOrder_OrderId(Integer orderId);
}