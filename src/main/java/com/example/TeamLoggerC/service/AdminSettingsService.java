package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.repository.AdminSettingsRepo;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class AdminSettingsService {

    private final AdminSettingsRepo adminSettingsRepo;

    public AdminSettingsService(AdminSettingsRepo adminSettingsRepo) {
        this.adminSettingsRepo = adminSettingsRepo;
    }

    public AdminSettings getSettings(Integer adminId) {

        return adminSettingsRepo
                .findByAdminId(adminId)
                .orElseGet(() -> createDefaultSettings(adminId));
    }

    private AdminSettings createDefaultSettings(Integer adminId) {

        AdminSettings settings = new AdminSettings();

        settings.setAdminId(adminId);
        settings.setCompanyName("My Company");

        settings.setWorkingStartTime(
                LocalTime.of(10, 0)
        );

        settings.setWorkingEndTime(
                LocalTime.of(19, 0)
        );

        settings.setIdleThresholdSeconds(10);

        // IP security defaults
        settings.setIpRestrictionEnabled(false);
        settings.setCompanyIpAddress(null);
        settings.setUnknownIpRequiresApproval(true);

        return adminSettingsRepo.save(settings);
    }

    public AdminSettings saveSettings(
            Integer adminId,
            String companyName,
            LocalTime workingStartTime,
            LocalTime workingEndTime,
            Integer idleThresholdSeconds,
            Boolean ipRestrictionEnabled,
            String companyIpAddress,
            Boolean unknownIpRequiresApproval) {

        // ---------------------------------------------
        // Company name
        // ---------------------------------------------

        if (companyName == null || companyName.isBlank()) {
            companyName = "My Company";
        }

        companyName = companyName.trim();


        // ---------------------------------------------
        // Working start time
        // ---------------------------------------------

        if (workingStartTime == null) {
            workingStartTime = LocalTime.of(10, 0);
        }


        // ---------------------------------------------
        // Working end time
        // ---------------------------------------------

        if (workingEndTime == null) {
            workingEndTime = LocalTime.of(19, 0);
        }

        if (!workingEndTime.isAfter(workingStartTime)) {
            throw new IllegalArgumentException(
                    "Working end time must be after working start time."
            );
        }


        // ---------------------------------------------
        // Idle threshold
        // ---------------------------------------------

        if (idleThresholdSeconds == null || idleThresholdSeconds <= 0) {
            idleThresholdSeconds = 10;
        }


        // ---------------------------------------------
        // IP restriction
        // ---------------------------------------------

        if (ipRestrictionEnabled == null) {
            ipRestrictionEnabled = false;
        }


        // ---------------------------------------------
        // Company IP
        // ---------------------------------------------

        if (companyIpAddress != null) {

            companyIpAddress = companyIpAddress.trim();

            if (companyIpAddress.isBlank()) {
                companyIpAddress = null;
            }
        }


        // ---------------------------------------------
        // Unknown IP approval
        // ---------------------------------------------

        if (unknownIpRequiresApproval == null) {
            unknownIpRequiresApproval = true;
        }


        // ---------------------------------------------
        // Load existing settings
        // ---------------------------------------------

        AdminSettings settings =
                adminSettingsRepo.findByAdminId(adminId)
                        .orElseGet(() -> createDefaultSettings(adminId));


        // ---------------------------------------------
        // Update settings
        // ---------------------------------------------

        settings.setCompanyName(companyName);

        settings.setWorkingStartTime(
                workingStartTime
        );

        settings.setWorkingEndTime(
                workingEndTime
        );

        settings.setIdleThresholdSeconds(
                idleThresholdSeconds
        );

        settings.setIpRestrictionEnabled(
                ipRestrictionEnabled
        );

        settings.setCompanyIpAddress(
                companyIpAddress
        );

        settings.setUnknownIpRequiresApproval(
                unknownIpRequiresApproval
        );


        // ---------------------------------------------
        // Save
        // ---------------------------------------------

        return adminSettingsRepo.save(settings);
    }
}