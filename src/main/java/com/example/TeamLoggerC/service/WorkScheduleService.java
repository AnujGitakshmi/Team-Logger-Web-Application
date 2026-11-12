
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AdminSettings;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class WorkScheduleService {

    private final AdminSettingsService adminSettingsService;

    public WorkScheduleService(
            AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    // ==========================================
    // GET ADMIN OFFICE START TIME
    // ==========================================

    public LocalTime getOfficeStartTime(Integer adminId) {

        AdminSettings settings =
                adminSettingsService.getSettings(adminId);

        return settings.getWorkingStartTime();
    }

    // ==========================================
    // GET ADMIN OFFICE END TIME
    // ==========================================

    public LocalTime getOfficeEndTime(Integer adminId) {

        AdminSettings settings =
                adminSettingsService.getSettings(adminId);

        return settings.getWorkingEndTime();
    }

    // ==========================================
    // CHECK CURRENT OFFICE HOURS
    // ==========================================

    public boolean isOfficeHours(Integer adminId) {

        return isOfficeHours(
                adminId,
                LocalDateTime.now()
        );
    }

    // ==========================================
    // CHECK OFFICE HOURS AT PARTICULAR TIME
    // ==========================================

    public boolean isOfficeHours(
            Integer adminId,
            LocalDateTime dateTime) {

        LocalTime start = getOfficeStartTime(adminId);
        LocalTime end = getOfficeEndTime(adminId);

        LocalTime current = dateTime.toLocalTime();

        return !current.isBefore(start)
                && current.isBefore(end);
    }

    // ==========================================
    // GET OFFICE START DATETIME
    // ==========================================

    public LocalDateTime getOfficeStart(Integer adminId,
                                        LocalDate date) {

        return LocalDateTime.of(
                date,
                getOfficeStartTime(adminId)
        );
    }

    // ==========================================
    // GET OFFICE END DATETIME
    // ==========================================

    public LocalDateTime getOfficeEnd(Integer adminId,
                                      LocalDate date) {

        return LocalDateTime.of(
                date,
                getOfficeEndTime(adminId)
        );
    }

    // ==========================================
    // CHECK BEFORE OFFICE START
    // ==========================================

    public boolean isBeforeOfficeStart(
            Integer adminId,
            LocalDateTime dateTime) {

        return dateTime.toLocalTime()
                .isBefore(getOfficeStartTime(adminId));
    }

    // ==========================================
    // CHECK AFTER OFFICE END
    // ==========================================

    public boolean isAfterOfficeEnd(
            Integer adminId,
            LocalDateTime dateTime) {

        return !dateTime.toLocalTime()
                .isBefore(getOfficeEndTime(adminId));
    }
}
