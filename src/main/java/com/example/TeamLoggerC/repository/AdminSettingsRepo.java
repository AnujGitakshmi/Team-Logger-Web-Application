package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.AdminSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSettingsRepo
        extends JpaRepository<AdminSettings, Integer> {

    Optional<AdminSettings> findByAdminId(Integer adminId);
}