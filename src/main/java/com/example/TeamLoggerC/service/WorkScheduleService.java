package com.example.TeamLoggerC.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class WorkScheduleService {

    // ==========================================
    // OFFICE WORKING HOURS
    // 10:00 AM to 7:00 PM
    // ==========================================

    private static final LocalTime OFFICE_START =
            LocalTime.of(10, 0);

    private static final LocalTime OFFICE_END =
            LocalTime.of(19, 0);


    // ==========================================
    // CHECK CURRENT OFFICE HOURS
    // ==========================================

    public boolean isOfficeHours() {

        LocalTime now = LocalTime.now();

        return !now.isBefore(OFFICE_START)
                && now.isBefore(OFFICE_END);
    }


    // ==========================================
    // CHECK A PARTICULAR DATE/TIME
    // ==========================================

    public boolean isOfficeHours(LocalDateTime dateTime) {

        LocalTime time = dateTime.toLocalTime();

        return !time.isBefore(OFFICE_START)
                && time.isBefore(OFFICE_END);
    }


    // ==========================================
    // GET OFFICE START DATETIME
    // ==========================================

    public LocalDateTime getOfficeStart(LocalDate date) {

        return LocalDateTime.of(
                date,
                OFFICE_START
        );
    }


    // ==========================================
    // GET OFFICE END DATETIME
    // ==========================================

    public LocalDateTime getOfficeEnd(LocalDate date) {

        return LocalDateTime.of(
                date,
                OFFICE_END
        );
    }


    // ==========================================
    // GET OFFICE START TIME
    // ==========================================

    public LocalTime getOfficeStartTime() {

        return OFFICE_START;
    }


    // ==========================================
    // GET OFFICE END TIME
    // ==========================================

    public LocalTime getOfficeEndTime() {

        return OFFICE_END;
    }
}