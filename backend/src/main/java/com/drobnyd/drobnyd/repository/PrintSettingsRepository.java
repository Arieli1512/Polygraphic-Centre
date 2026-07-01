package com.drobnyd.drobnyd.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.PrintSettings;

public interface PrintSettingsRepository extends JpaRepository<PrintSettings, Integer> {
}