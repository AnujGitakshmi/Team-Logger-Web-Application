package com.example.TeamLoggerC.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeDailyReportDTO {

    private LocalDate date;

    private LocalDateTime loginTime;

    private LocalDateTime logoutTime;

    private int activeMinutes;

    private int idleMinutes;

    private Double productivity;

    private int overtimeMinutes;

    private String status;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeDailyReportDTO() {
    }


    public EmployeeDailyReportDTO(
            LocalDate date,
            LocalDateTime loginTime,
            LocalDateTime logoutTime,
            int activeMinutes,
            int idleMinutes,
            Double productivity,
            int overtimeMinutes,
            String status) {

        this.date = date;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.activeMinutes = activeMinutes;
        this.idleMinutes = idleMinutes;
        this.productivity = productivity;
        this.overtimeMinutes = overtimeMinutes;
        this.status = status;
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public LocalDate getDate() {
        return date;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public int getActiveMinutes() {
        return activeMinutes;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    public Double getProductivity() {
        return productivity;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public String getStatus() {
        return status;
    }


    // =====================================================
    // SETTERS
    // =====================================================

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public void setActiveMinutes(int activeMinutes) {
        this.activeMinutes = activeMinutes;
    }

    public void setIdleMinutes(int idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    public void setProductivity(Double productivity) {
        this.productivity = productivity;
    }

    public void setOvertimeMinutes(int overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // =====================================================
    // FORMATTED VALUES FOR THYMELEAF
    // =====================================================

    public String getActiveTimeFormatted() {

        int hours = activeMinutes / 60;
        int minutes = activeMinutes % 60;

        return hours + "h " + minutes + "m";
    }


    public String getIdleTimeFormatted() {

        int hours = idleMinutes / 60;
        int minutes = idleMinutes % 60;

        return hours + "h " + minutes + "m";
    }


    public String getOvertimeFormatted() {

        int hours = overtimeMinutes / 60;
        int minutes = overtimeMinutes % 60;

        return hours + "h " + minutes + "m";
    }


    public String getProductivityFormatted() {

        if (productivity == null) {
            return "—";
        }

        return String.format(
                "%.1f%%",
                productivity
        );
    }
}

