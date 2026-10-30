package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.ScreenshotSettings;
import com.example.TeamLoggerC.repository.ScreenshotSettingsRepo;
import org.springframework.stereotype.Service;

@Service
public class ScreenshotSettingsService {

    private final ScreenshotSettingsRepo settingsRepo;


    public ScreenshotSettingsService(
            ScreenshotSettingsRepo settingsRepo) {

        this.settingsRepo = settingsRepo;
    }


    // =========================================================
    // GET SETTINGS
    // =========================================================

    public ScreenshotSettings getSettings(Integer adminId) {

        return settingsRepo.findByAdminId(adminId)
                .orElseGet(() -> createDefaultSettings(adminId));
    }


    // =========================================================
    // CREATE DEFAULT SETTINGS
    // =========================================================

    private ScreenshotSettings createDefaultSettings(
            Integer adminId) {

        ScreenshotSettings settings =
                new ScreenshotSettings();

        settings.setAdminId(adminId);
        settings.setEnabled(true);
        settings.setIntervalMinutes(5);

        return settingsRepo.save(settings);
    }


    // =========================================================
    // SAVE SETTINGS
    // =========================================================

    public ScreenshotSettings saveSettings(
            Integer adminId,
            Boolean enabled,
            Integer intervalMinutes) {

        ScreenshotSettings settings =
                getSettings(adminId);

        settings.setEnabled(enabled);
        settings.setIntervalMinutes(intervalMinutes);

        return settingsRepo.save(settings);
    }


    // =========================================================
    // CHECK ENABLED
    // =========================================================

    public boolean isEnabled(Integer adminId) {

        ScreenshotSettings settings =
                getSettings(adminId);

        return Boolean.TRUE.equals(settings.getEnabled());
    }


    // =========================================================
    // GET INTERVAL
    // =========================================================

    public int getIntervalMinutes(Integer adminId) {

        ScreenshotSettings settings =
                getSettings(adminId);

        return settings.getIntervalMinutes();
    }
}