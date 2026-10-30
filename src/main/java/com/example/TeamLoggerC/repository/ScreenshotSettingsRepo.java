package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.ScreenshotSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScreenshotSettingsRepo
        extends JpaRepository<ScreenshotSettings, Integer> {

    Optional<ScreenshotSettings> findByAdminId(Integer adminId);


}