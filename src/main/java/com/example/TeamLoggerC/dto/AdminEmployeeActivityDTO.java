package com.example.TeamLoggerC.dto;

public class AdminEmployeeActivityDTO {

    private Integer id;
    private String name;
    private String designation;
    private String status;

    private boolean workingNow;

    private long activeSeconds;
    private long idleSeconds;

    private String activeTime;
    private String idleTime;

    private String currentState;

    public AdminEmployeeActivityDTO() {
    }

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

    public long getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(long activeSeconds) {
        this.activeSeconds = activeSeconds;
    }

    public long getIdleSeconds() {
        return idleSeconds;
    }

    public void setIdleSeconds(long idleSeconds) {
        this.idleSeconds = idleSeconds;
    }

    public String getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(String activeTime) {
        this.activeTime = activeTime;
    }

    public String getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(String idleTime) {
        this.idleTime = idleTime;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
}