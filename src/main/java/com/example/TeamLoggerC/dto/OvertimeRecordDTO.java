package com.example.TeamLoggerC.dto;

import java.time.LocalDate;

public class OvertimeRecordDTO {

    private LocalDate date;
    private int activeMinutes;
    private int overtimeMinutes;

    public OvertimeRecordDTO(
            LocalDate date,
            int activeMinutes,
            int overtimeMinutes) {

        this.date = date;
        this.activeMinutes = activeMinutes;
        this.overtimeMinutes = overtimeMinutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getActiveMinutes() {
        return activeMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public int getActiveHours() {
        return activeMinutes / 60;
    }

    public int getActiveRemainingMinutes() {
        return activeMinutes % 60;
    }

    public int getOvertimeHours() {
        return overtimeMinutes / 60;
    }

    public int getOvertimeRemainingMinutes() {
        return overtimeMinutes % 60;
    }
}