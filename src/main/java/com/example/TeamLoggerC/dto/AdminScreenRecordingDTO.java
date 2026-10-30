package com.example.TeamLoggerC.dto;

import java.time.LocalDateTime;

public class AdminScreenRecordingDTO {

    private Integer id;
    private String name;
    private String designation;
    private String status;

    private boolean workingNow;
    private boolean employeeStarted;
    private boolean recording;

    private Integer sessionId;
    private LocalDateTime employeeStartedAt;

    public AdminScreenRecordingDTO() {
    }

    // ==========================================================
    // GETTERS AND SETTERS
    // ==========================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isWorkingNow() {
        return workingNow;
    }

    public void setWorkingNow(boolean workingNow) {
        this.workingNow = workingNow;
    }

    public boolean isEmployeeStarted() {
        return employeeStarted;
    }

    public void setEmployeeStarted(boolean employeeStarted) {
        this.employeeStarted = employeeStarted;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getEmployeeStartedAt() {
        return employeeStartedAt;
    }

    public void setEmployeeStartedAt(LocalDateTime employeeStartedAt) {
        this.employeeStartedAt = employeeStartedAt;
    }
}